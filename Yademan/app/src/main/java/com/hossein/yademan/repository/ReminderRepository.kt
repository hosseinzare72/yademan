// مسیر: app/src/main/java/com/hossein/yademan/repository/ReminderRepository.kt
package com.hossein.yademan.repository

import android.content.Context
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.data.Reminder
import com.hossein.yademan.data.ReminderDao
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.data.ReminderType
import com.hossein.yademan.data.SyncStatus
import com.hossein.yademan.services.AlarmScheduler
import com.hossein.yademan.services.SyncWorker
import com.hossein.yademan.utils.DateUtils
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * مخزن یادآورها. قانون‌ها:
 * - هر تغییر محلی: version+1، updated_at=الان، sync_status=local_changed (یا local_new اگر هنوز سینک نشده)
 * - حذف: اگر هیچ‌وقت سینک نشده، حذف واقعی؛ وگرنه is_deleted=1 و local_deleted تا به سرور push شود
 * - بعد از هر تغییر: آلارم بازچینی و سینکِ debounced درخواست می‌شود
 */
class ReminderRepository(private val dao: ReminderDao, private val context: Context) {

    fun observeAll(): Flow<List<Reminder>> = dao.observeAll()
    fun observeById(id: String): Flow<Reminder?> = dao.observeById(id)
    suspend fun getAll(): List<Reminder> = dao.getAll()
    suspend fun getById(id: String): Reminder? = dao.getById(id)

    /** قالب رکورد جدید برای یک نوع */
    fun newReminder(type: String, title: String, dueAtMillis: Long): Reminder {
        val now = DateUtils.nowIso()
        return Reminder(
            id = UUID.randomUUID().toString(),
            title = title,
            dueAt = DateUtils.millisToIso(dueAtMillis),
            categoryId = ReminderType.categoryId(type),
            color = ReminderType.colorHex(type),
            metaJson = ReminderMeta.build(type),
            createdAt = now,
            updatedAt = now,
            deviceId = ServiceLocator.auth.deviceId,
            version = 1,
            syncStatus = SyncStatus.LOCAL_NEW
        )
    }

    suspend fun create(reminder: Reminder) {
        val rec = reminder.copy(
            updatedAt = DateUtils.nowIso(),
            deviceId = ServiceLocator.auth.deviceId,
            syncStatus = SyncStatus.LOCAL_NEW,
            isDeleted = false
        )
        dao.upsert(rec)
        afterChange(rec)
    }

    suspend fun update(reminder: Reminder) {
        val current = dao.getById(reminder.id)
        val status = if (current == null || current.syncStatus == SyncStatus.LOCAL_NEW) {
            SyncStatus.LOCAL_NEW
        } else {
            SyncStatus.LOCAL_CHANGED
        }
        val rec = reminder.copy(
            updatedAt = DateUtils.nowIso(),
            version = (current?.version ?: reminder.version) + 1,
            deviceId = ServiceLocator.auth.deviceId,
            syncStatus = status,
            isDeleted = false
        )
        dao.upsert(rec)
        afterChange(rec)
    }

    /** انجام/لغو انجام برای یک روز (تکراری) یا کل یادآور (یک‌بار) */
    suspend fun setDoneOn(reminder: Reminder, dayKey: String, done: Boolean) {
        val fresh = dao.getById(reminder.id) ?: return
        update(ReminderMeta.withDoneOn(fresh, dayKey, done))
    }

    suspend fun delete(id: String) {
        val current = dao.getById(id) ?: return
        if (current.syncStatus == SyncStatus.LOCAL_NEW) {
            dao.hardDelete(id)
        } else {
            dao.upsert(
                current.copy(
                    isDeleted = true,
                    syncStatus = SyncStatus.LOCAL_DELETED,
                    version = current.version + 1,
                    updatedAt = DateUtils.nowIso()
                )
            )
        }
        AlarmScheduler.cancel(context, id)
        SyncWorker.requestSoon(context)
    }

    suspend fun deleteMany(ids: List<String>) {
        ids.forEach { delete(it) }
    }

    private suspend fun afterChange(rec: Reminder) {
        AlarmScheduler.schedule(context, rec)
        SyncWorker.requestSoon(context)
    }

    // ---------- فقط برای SyncManager و Seed (بدون درخواست سینک مجدد) ----------
    suspend fun applyRemote(reminder: Reminder) = dao.upsert(reminder)
    suspend fun insertSeed(list: List<Reminder>) = dao.upsertAll(list)
    suspend fun getDirty(): List<Reminder> = dao.getDirty()
    suspend fun getPendingDeletes(): List<Reminder> = dao.getPendingDeletes()
    suspend fun markSyncedIfUnchanged(id: String, updatedAt: String) = dao.markSyncedIfUnchanged(id, updatedAt)
    suspend fun purgeDeleted(ids: List<String>) = dao.purgeDeleted(ids)
    suspend fun hardDelete(id: String) = dao.hardDelete(id)
}
