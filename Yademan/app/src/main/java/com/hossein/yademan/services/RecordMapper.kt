// مسیر: app/src/main/java/com/hossein/yademan/services/RecordMapper.kt
package com.hossein.yademan.services

import com.google.gson.JsonObject
import com.hossein.yademan.data.Category
import com.hossein.yademan.data.Reminder
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.data.ReminderType
import com.hossein.yademan.data.SyncStatus
import com.hossein.yademan.utils.DateUtils

/**
 * تبدیل Entity ↔ رکورد سینک.
 *
 * نگاشت record_type (سرور فقط این‌ها را قبول می‌کند: reminder, task, subtask, category, tag, note,
 * habit, habit_log, goal, event, routine, setting):
 *   یادداشت → note | قرار → event | قلم خرید → task | قسط/چک/دارو/سایر → reminder | دسته → category
 * record_data همیشه «کل» ردیف با نام ستون‌های snake_case است؛ پس هیچ فیلدی در رفت‌وبرگشت گم نمی‌شود
 * و نوع دقیق هم در record_data.meta_json.type حفظ می‌شود.
 */
object RecordMapper {

    val REMINDER_TYPES = setOf("reminder", "task", "note", "event")

    fun recordTypeFor(r: Reminder): String = when (ReminderMeta.type(r)) {
        ReminderType.NOTE -> "note"
        ReminderType.MEETING -> "event"
        ReminderType.SHOPPING -> "task"
        else -> "reminder"
    }

    fun reminderData(r: Reminder): JsonObject = JsonObject().apply {
        addProperty("id", r.id)
        addProperty("title", r.title)
        addProperty("note", r.note)
        addProperty("due_at", r.dueAt)
        addProperty("all_day", r.allDay)
        addProperty("repeat_rule", r.repeatRule)
        addProperty("repeat_interval", r.repeatInterval)
        addProperty("weekdays_mask", r.weekdaysMask)
        addProperty("end_at", r.endAt)
        addProperty("priority", r.priority)
        addProperty("category_id", r.categoryId)
        addProperty("tags_json", r.tagsJson)
        addProperty("is_done", r.isDone)
        addProperty("done_at", r.doneAt)
        addProperty("alarm_enabled", r.alarmEnabled)
        addProperty("alarm_offset_minutes", r.alarmOffsetMinutes)
        addProperty("sms_reminder", r.smsReminder)
        addProperty("email_reminder", r.emailReminder)
        addProperty("color", r.color)
        addProperty("sort_order", r.sortOrder)
        addProperty("amount_toman", r.amountToman)
        addProperty("meta_json", r.metaJson)
        addProperty("created_at", r.createdAt)
        addProperty("updated_at", r.updatedAt)
        addProperty("device_id", r.deviceId)
        addProperty("version", r.version)
    }

    fun reminderRecord(r: Reminder): JsonObject = JsonObject().apply {
        addProperty("id", r.id)
        addProperty("record_type", recordTypeFor(r))
        add("record_data", reminderData(r))
        addProperty("version", r.version)
        addProperty("updated_at", r.updatedAt)
        addProperty("device_id", r.deviceId)
        addProperty("is_deleted", false)
    }

    fun categoryRecord(c: Category): JsonObject = JsonObject().apply {
        addProperty("id", c.id)
        addProperty("record_type", "category")
        add("record_data", JsonObject().apply {
            addProperty("id", c.id)
            addProperty("name", c.name)
            addProperty("color", c.color)
            addProperty("sort_order", c.sortOrder)
            addProperty("created_at", c.createdAt)
            addProperty("updated_at", c.updatedAt)
            addProperty("device_id", c.deviceId)
            addProperty("version", c.version)
        })
        addProperty("version", c.version)
        addProperty("updated_at", c.updatedAt)
        addProperty("device_id", c.deviceId)
        addProperty("is_deleted", false)
    }

    /** شناسه رکورد (سرور ممکن است id یا record_id بفرستد) */
    fun recordId(rec: JsonObject): String? =
        rec.str("record_id") ?: rec.str("id") ?: rec.obj("record_data")?.str("id")

    fun recordVersion(rec: JsonObject): Int =
        rec.int("version") ?: rec.obj("record_data")?.int("version") ?: 1

    fun isDeletedRecord(rec: JsonObject): Boolean =
        rec.bool("is_deleted") ?: rec.bool("deleted") ?: false

    /** رکورد سرور → Reminder (با sync_status=synced) */
    fun reminderFrom(rec: JsonObject): Reminder? {
        val id = recordId(rec) ?: return null
        val d = rec.obj("record_data") ?: JsonObject()
        val now = DateUtils.nowIso()
        val updated = rec.str("updated_at") ?: d.str("updated_at") ?: now
        var meta = d.str("meta_json") ?: d.obj("meta")?.toString() ?: d.str("meta") ?: "{}"
        // اگر meta نوع نداشت، از record_type حدس می‌زنیم
        val metaObj = ReminderMeta.parse(meta)
        if (!metaObj.has("type")) {
            val guessed = when (rec.str("record_type")) {
                "note" -> ReminderType.NOTE
                "event" -> ReminderType.MEETING
                "task" -> ReminderType.SHOPPING
                else -> ReminderType.fromCategory(d.str("category_id"))
            }
            metaObj.addProperty("type", guessed)
            meta = metaObj.toString()
        }
        return Reminder(
            id = id,
            title = d.str("title") ?: "",
            note = d.str("note") ?: "",
            dueAt = d.str("due_at") ?: now,
            allDay = d.bool("all_day") ?: false,
            repeatRule = d.str("repeat_rule") ?: "none",
            repeatInterval = d.int("repeat_interval") ?: 1,
            weekdaysMask = d.int("weekdays_mask") ?: 0,
            endAt = d.str("end_at"),
            priority = d.int("priority") ?: 1,
            categoryId = d.str("category_id"),
            tagsJson = d.str("tags_json"),
            isDone = d.bool("is_done") ?: false,
            doneAt = d.str("done_at"),
            alarmEnabled = d.bool("alarm_enabled") ?: true,
            alarmOffsetMinutes = d.int("alarm_offset_minutes") ?: 0,
            smsReminder = d.bool("sms_reminder") ?: false,
            emailReminder = d.bool("email_reminder") ?: false,
            color = d.str("color") ?: "#D9B45B",
            sortOrder = d.int("sort_order") ?: 0,
            amountToman = d.long("amount_toman") ?: 0L,
            metaJson = meta,
            createdAt = d.str("created_at") ?: updated,
            updatedAt = updated,
            deviceId = d.str("device_id") ?: rec.str("device_id") ?: "server",
            version = recordVersion(rec),
            syncStatus = SyncStatus.SYNCED,
            isDeleted = false
        )
    }

    fun categoryFrom(rec: JsonObject): Category? {
        val id = recordId(rec) ?: return null
        val d = rec.obj("record_data") ?: JsonObject()
        val now = DateUtils.nowIso()
        val updated = rec.str("updated_at") ?: d.str("updated_at") ?: now
        return Category(
            id = id,
            name = d.str("name") ?: "",
            color = d.str("color") ?: "#D9B45B",
            sortOrder = d.int("sort_order") ?: 0,
            createdAt = d.str("created_at") ?: updated,
            updatedAt = updated,
            deviceId = d.str("device_id") ?: "server",
            version = recordVersion(rec),
            syncStatus = SyncStatus.SYNCED,
            isDeleted = false
        )
    }
}
