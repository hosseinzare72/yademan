// مسیر: app/src/main/java/com/hossein/yademan/services/ReminderReceiver.kt
package com.hossein.yademan.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.data.Reminder
import com.hossein.yademan.data.ReminderMeta
import com.hossein.yademan.data.ReminderType
import com.hossein.yademan.repository.MetaKeys
import com.hossein.yademan.utils.DateUtils
import com.hossein.yademan.utils.PersianDigits
import com.hossein.yademan.utils.Recurrence
import com.hossein.yademan.utils.fa
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * دریافت آلارم (حتی با اپ بسته/صفحه خاموش/Doze):
 * ۱) نمایش نوتیفیکیشن روی کانال yademan_reminders
 * ۲) ثبت در notification_log
 * ۳) زمان‌بندی وقوع بعدی (برای تکراری‌ها) + یادآوری دوباره در حالت «هوشمند»
 * goAsync باعث می‌شود پروسه تا پایان کار دیتابیس زنده بماند.
 * همچنین دکمه «انجام شد» روی نوتیفیکیشن را هم مدیریت می‌کند.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        ServiceLocator.appScope.launch {
            try {
                when (intent.action) {
                    AlarmScheduler.ACTION_FIRE -> onFire(appContext, intent)
                    AlarmScheduler.ACTION_DONE -> onDone(appContext, intent)
                }
            } catch (e: Exception) {
                // هیچ خطایی نباید پروسه را در پس‌زمینه کرش دهد
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun onFire(context: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: return
        val followUp = intent.getBooleanExtra(AlarmScheduler.EXTRA_FOLLOW_UP, false)
        val occurrence = intent.getLongExtra(AlarmScheduler.EXTRA_OCCURRENCE, System.currentTimeMillis())
        val meta = ServiceLocator.meta
        val silent = !meta.getBool(MetaKeys.NOTIF_SOUND, true) ||
            (meta.getBool(MetaKeys.NOTIF_DND, false) && isQuietHours())

        if (id == AlarmScheduler.TEST_ID) {
            val title = "اعلان آزمایشی یادمان"
            val body = "اگر این را می‌بینی، اعلان‌ها با اپ بسته هم کار می‌کنند ✓"
            NotificationHelper.show(context, id.hashCode(), title, body, silent, null, occurrence, null)
            ServiceLocator.notifications.log(title, body, null)
            return
        }

        val r = ServiceLocator.reminders.getById(id) ?: return
        if (r.isDeleted || !r.alarmEnabled) return
        val dayKey = DateUtils.dayKey(occurrence)

        if (followUp) {
            if (!ReminderMeta.isDoneOn(r, dayKey)) {
                val title = "هنوز انجام نشده: ${r.title}"
                val body = bodyFor(r, occurrence)
                NotificationHelper.show(context, id.hashCode(), title, body, silent, id, occurrence, doneLabel(r))
                ServiceLocator.notifications.log(title, body, id)
            }
            return
        }

        // اگر قبلاً (مثلاً زودتر از موعد) انجام شده، فقط وقوع بعدی را می‌چینیم
        if (!ReminderMeta.isDoneOn(r, dayKey)) {
            val title = r.title
            val body = bodyFor(r, occurrence)
            NotificationHelper.show(context, id.hashCode(), title, body, silent, id, occurrence, doneLabel(r))
            ServiceLocator.notifications.log(title, body, id)
            if (meta.getBool(MetaKeys.NOTIF_SMART, true)) {
                AlarmScheduler.scheduleFollowUp(context, id, occurrence)
            }
        }
        meta.set(MetaKeys.FIRED_PREFIX + id, occurrence.toString())
        AlarmScheduler.schedule(context, r)
    }

    private suspend fun onDone(context: Context, intent: Intent) {
        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: return
        val occurrence = intent.getLongExtra(AlarmScheduler.EXTRA_OCCURRENCE, System.currentTimeMillis())
        val r = ServiceLocator.reminders.getById(id) ?: return
        ServiceLocator.reminders.setDoneOn(r, DateUtils.dayKey(occurrence), true)
        AlarmScheduler.cancelFollowUp(context, id)
        NotificationHelper.cancel(context, id.hashCode())
    }

    private fun doneLabel(r: Reminder): String = when (ReminderMeta.type(r)) {
        ReminderType.MEDICINE -> "مصرف شد"
        ReminderType.INSTALLMENT, ReminderType.CHECK -> "پرداخت شد"
        ReminderType.SUBSCRIPTION -> "تمدید شد"
        else -> "انجام شد"
    }

    /** متن بدنه بر اساس نوع */
    private fun bodyFor(r: Reminder, occurrence: Long): String {
        val time = DateUtils.timeFa(occurrence)
        val date = DateUtils.dateShortFa(occurrence)
        val whenText = if (DateUtils.epochDay(occurrence) == DateUtils.epochDay(System.currentTimeMillis())) {
            "امروز ساعت $time"
        } else {
            "$date ساعت $time"
        }
        return when (ReminderMeta.type(r)) {
            ReminderType.MEDICINE -> {
                val dose = ReminderMeta.str(r, "dose")
                if (dose.isNullOrBlank()) "وقت مصرف دارو • $time" else "وقت مصرف: $dose • $time"
            }
            ReminderType.INSTALLMENT, ReminderType.CHECK -> {
                val amount = if (r.amountToman > 0) PersianDigits.toman(r.amountToman) + " • " else ""
                val count = ReminderMeta.totalCount(r)
                val idx = if (count != null) Recurrence.occurrenceIndex(r, occurrence) else null
                val progress = if (count != null && idx != null) {
                    if (idx >= count) " • آخرین قسط!" else " • قسط ${idx.fa()} از ${count.fa()}"
                } else ""
                "${amount}سررسید $whenText$progress"
            }
            ReminderType.SUBSCRIPTION -> {
                val amount = if (r.amountToman > 0) PersianDigits.toman(r.amountToman) + " • " else ""
                val plan = ReminderMeta.str(r, "plan")?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
                "${amount}تمدید اشتراک $whenText$plan"
            }
            ReminderType.MEETING -> {
                val loc = ReminderMeta.str(r, "location")
                if (loc.isNullOrBlank()) "قرار $whenText" else "قرار $whenText • $loc"
            }
            else -> r.note.takeIf { it.isNotBlank() } ?: "یادآوری $whenText"
        }
    }

    /** بازه «مزاحم نشو»: ۲۳ تا ۷ صبح */
    private fun isQuietHours(): Boolean {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return h >= 23 || h < 7
    }
}
