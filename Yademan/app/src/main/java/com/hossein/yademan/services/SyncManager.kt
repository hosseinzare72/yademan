// مسیر: app/src/main/java/com/hossein/yademan/services/SyncManager.kt
package com.hossein.yademan.services

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.data.SyncStatus
import com.hossein.yademan.repository.MetaKeys
import com.hossein.yademan.utils.DateUtils
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/** نتیجه یک دور سینک */
sealed class SyncResult {
    data object Success : SyncResult()
    data object NotLoggedIn : SyncResult()
    data class NetworkError(val message: String) : SyncResult()
    data class ServerError(val message: String, val authFailed: Boolean) : SyncResult()
}

/**
 * سینک دوطرفه offline-first.
 * ۱) push: رکوردهای local_new/local_changed + شناسه‌های local_deleted
 * ۲) رسیدگی به conflicts: version بزرگ‌تر برنده؛ برابر → سرور برنده
 * ۳) pull از last_sync و ادغام با همان قانون
 * ۴) ذخیره last_sync برگشتی سرور + بازچینش آلارم‌ها
 * با Mutex هیچ‌وقت دو سینک هم‌زمان اجرا نمی‌شود.
 */
object SyncManager {

    /** مقدار last_sync در اولین سینک (قابل تغییر اگر پلاگین سرور فرمت دیگری بخواهد) */
    const val INITIAL_LAST_SYNC = "1970-01-01 00:00:00"

    private val mutex = Mutex()

    suspend fun sync(): SyncResult = mutex.withLock {
        val auth = ServiceLocator.auth
        val token = auth.token
        if (token.isNullOrBlank()) return@withLock SyncResult.NotLoggedIn
        val meta = ServiceLocator.meta
        try {
            val deviceId = auth.deviceId
            val lastSync = meta.get(MetaKeys.LAST_SYNC) ?: INITIAL_LAST_SYNC

            val forceFullPull = push(token, deviceId, lastSync)
            pull(token, deviceId, if (forceFullPull) INITIAL_LAST_SYNC else lastSync, lastSync)

            meta.set(MetaKeys.LAST_SYNC_LOCAL, DateUtils.nowIso())
            meta.delete(MetaKeys.LAST_SYNC_ERROR)
            AlarmScheduler.rescheduleAll(ServiceLocator.app)
            SyncResult.Success
        } catch (e: ApiException) {
            val authFailed = e.httpCode == 401 || e.httpCode == 403
            meta.set(MetaKeys.LAST_SYNC_ERROR, e.message ?: "خطای سرور")
            SyncResult.ServerError(e.message ?: "خطای سرور", authFailed)
        } catch (e: IOException) {
            meta.set(MetaKeys.LAST_SYNC_ERROR, "اتصال به اینترنت برقرار نیست")
            SyncResult.NetworkError(e.message ?: "network")
        } catch (e: Exception) {
            meta.set(MetaKeys.LAST_SYNC_ERROR, e.message ?: "خطای ناشناخته")
            SyncResult.ServerError(e.message ?: "خطای ناشناخته", false)
        }
    }

    /** @return true اگر برای حل تعارض لازم باشد pull کامل انجام شود */
    private suspend fun push(token: String, deviceId: String, lastSync: String): Boolean {
        val reminders = ServiceLocator.reminders
        val categories = ServiceLocator.categories
        val dirtyR = reminders.getDirty()
        val dirtyC = categories.getDirty()
        val delR = reminders.getPendingDeletes()
        val delC = categories.getPendingDeletes()
        if (dirtyR.isEmpty() && dirtyC.isEmpty() && delR.isEmpty() && delC.isEmpty()) return false

        val records = JsonArray()
        dirtyC.forEach { records.add(RecordMapper.categoryRecord(it)) }
        dirtyR.forEach { records.add(RecordMapper.reminderRecord(it)) }
        val deleted = JsonArray()
        delR.forEach { deleted.add(it.id) }
        delC.forEach { deleted.add(it.id) }

        val resp = ApiClient.push(token, deviceId, records, deleted, lastSync)
        val conflicts = parseConflicts(resp)
        val conflictIds = conflicts.mapNotNull { conflictId(it) }.toSet()

        // فقط رکوردهایی که در حین سینک دوباره تغییر نکرده‌اند synced می‌شوند
        dirtyR.filter { it.id !in conflictIds }.forEach { reminders.markSyncedIfUnchanged(it.id, it.updatedAt) }
        dirtyC.filter { it.id !in conflictIds }.forEach { categories.markSyncedIfUnchanged(it.id, it.updatedAt) }
        reminders.purgeDeleted(delR.map { it.id }.filter { it !in conflictIds })
        categories.purgeDeleted(delC.map { it.id }.filter { it !in conflictIds })

        return if (conflicts.isNotEmpty()) resolveConflicts(token, conflicts) else false
    }

    private fun parseConflicts(resp: JsonObject): List<JsonObject> {
        val el = resp.get("conflicts") ?: return emptyList()
        return when {
            el.isJsonArray -> el.asJsonArray.mapNotNull { it.asObjectOrNull() ?: primitiveConflict(it.toString()) }
            el.isJsonObject -> el.asJsonObject.entrySet().mapNotNull { (key, value) ->
                (value.asObjectOrNull() ?: JsonObject()).apply { if (!has("id")) addProperty("id", key) }
            }
            else -> emptyList()
        }
    }

    /** اگر سرور فقط لیست شناسه‌ها را فرستاده باشد */
    private fun primitiveConflict(raw: String): JsonObject =
        JsonObject().apply { addProperty("id", raw.trim('"')) }

    private fun conflictId(c: JsonObject): String? =
        c.str("id") ?: c.str("record_id") ?: c.obj("server_record")?.let { RecordMapper.recordId(it) }

    /**
     * قانون: version بزرگ‌تر برنده؛ برابر → سرور.
     * @return true اگر نسخه سرور همراه تعارض نیامده و باید pull کامل شود
     */
    private suspend fun resolveConflicts(token: String, conflicts: List<JsonObject>): Boolean {
        val reminders = ServiceLocator.reminders
        val categories = ServiceLocator.categories
        val resolutions = JsonArray()
        var needFullPull = false
        for (c in conflicts) {
            val id = conflictId(c) ?: continue
            val serverRecord = c.obj("server_record") ?: c.obj("server") ?: c.obj("record")
            val serverVersion = c.int("server_version") ?: serverRecord?.let { RecordMapper.recordVersion(it) } ?: 0

            // تعارض دسته‌ها جدا رسیدگی می‌شود (قبلاً رکورد دسته به‌اشتباه به‌صورت Reminder ذخیره می‌شد)
            val localCategory = categories.getById(id)
            val isCategory = localCategory != null ||
                c.str("record_type") == "category" || serverRecord?.str("record_type") == "category"
            if (isCategory) {
                if (localCategory != null && localCategory.version > serverVersion) {
                    resolutions.add(JsonObject().apply {
                        addProperty("id", id)
                        addProperty("resolution", "client")
                        addProperty("winner", "client")
                        add("record", RecordMapper.categoryRecord(localCategory))
                    })
                    categories.markSyncedIfUnchanged(localCategory.id, localCategory.updatedAt)
                } else {
                    resolutions.add(JsonObject().apply {
                        addProperty("id", id)
                        addProperty("resolution", "server")
                        addProperty("winner", "server")
                    })
                    val remoteCat = serverRecord?.let { RecordMapper.categoryFrom(it.withId(id)) }
                    if (remoteCat != null) {
                        categories.applyRemote(remoteCat)
                    } else {
                        needFullPull = true
                        if (localCategory != null) categories.applyRemote(localCategory.copy(syncStatus = SyncStatus.SYNCED))
                    }
                }
                continue
            }

            val local = reminders.getById(id)
            if (local != null && local.version > serverVersion) {
                // نسخه محلی برنده است
                resolutions.add(JsonObject().apply {
                    addProperty("id", id)
                    addProperty("resolution", "client")
                    addProperty("winner", "client")
                    add("record", RecordMapper.reminderRecord(local))
                })
                reminders.markSyncedIfUnchanged(local.id, local.updatedAt)
            } else {
                // سرور برنده است
                resolutions.add(JsonObject().apply {
                    addProperty("id", id)
                    addProperty("resolution", "server")
                    addProperty("winner", "server")
                })
                val remote = serverRecord?.let { RecordMapper.reminderFrom(it.withId(id)) }
                if (remote != null) {
                    reminders.applyRemote(remote)
                } else {
                    needFullPull = true
                    // تا pull کامل نسخه سرور را بیاورد، رکورد محلی را synced می‌کنیم تا دوباره push نشود
                    if (local != null) reminders.applyRemote(local.copy(syncStatus = SyncStatus.SYNCED))
                }
            }
        }
        if (resolutions.size() > 0) ApiClient.resolve(token, resolutions)
        return needFullPull
    }

    private fun JsonObject.withId(id: String): JsonObject {
        if (!has("id") && !has("record_id")) addProperty("id", id)
        return this
    }

    private suspend fun pull(token: String, deviceId: String, since: String, previousLastSync: String) {
        val reminders = ServiceLocator.reminders
        val categories = ServiceLocator.categories
        val resp = ApiClient.pull(token, deviceId, since)
        val records = resp.arr("records") ?: JsonArray()
        for (el in records) {
            val rec = el.asObjectOrNull() ?: continue
            val id = RecordMapper.recordId(rec) ?: continue
            val type = rec.str("record_type") ?: "reminder"
            val remoteVersion = RecordMapper.recordVersion(rec)
            val deleted = RecordMapper.isDeletedRecord(rec)
            when {
                type == "category" -> {
                    val local = categories.getById(id)
                    val remoteWins = local == null || local.syncStatus == SyncStatus.SYNCED || remoteVersion >= local.version
                    if (!remoteWins) continue
                    if (deleted) categories.hardDelete(id)
                    else RecordMapper.categoryFrom(rec)?.let { categories.applyRemote(it) }
                }
                type in RecordMapper.REMINDER_TYPES -> {
                    val local = reminders.getById(id)
                    val remoteWins = local == null || local.syncStatus == SyncStatus.SYNCED || remoteVersion >= local.version
                    if (!remoteWins) continue
                    if (deleted) {
                        reminders.hardDelete(id)
                        AlarmScheduler.cancel(ServiceLocator.app, id)
                    } else {
                        RecordMapper.reminderFrom(rec)?.let { reminders.applyRemote(it) }
                    }
                }
                else -> Unit // انواع دیگر (habit, goal, ...) در این نسخه استفاده نمی‌شوند
            }
        }
        val newLastSync = resp.str("last_sync") ?: resp.str("server_time") ?: previousLastSync
        ServiceLocator.meta.set(MetaKeys.LAST_SYNC, newLastSync)
    }
}
