// مسیر: app/src/main/java/com/hossein/yademan/services/AlarmScheduler.kt
package com.hossein.yademan.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.hossein.yademan.ServiceLocator
import com.hossein.yademan.data.Reminder
import com.hossein.yademan.repository.MetaKeys
import com.hossein.yademan.utils.DateUtils
import com.hossein.yademan.utils.Recurrence
import kotlin.math.max

/**
 * زمان‌بندی آلارم‌های یادآور — مقاوم در برابر Doze و بسته بودن اپ.
 *
 * - برای هر یادآور فعال فقط «وقوع بعدی» زمان‌بندی می‌شود: زمان زنگ = وقوع − alarm_offset_minutes
 *   (اگر آن لحظه گذشته ولی خود وقوع نرسیده، بلافاصله زنگ می‌خورد)
 * - setExactAndAllowWhileIdle (در Doze هم اجرا می‌شود). اگر مجوز آلارم دقیق نبود → setAndAllowWhileIdle
 * - هر PendingIntent با data URI یکتا (yademan://reminder/{id}) ساخته می‌شود تا لغو دقیق کار کند
 * - بعد از زنگ خوردن، ReminderReceiver دوباره schedule را صدا می‌زند تا وقوع بعدیِ تکراری‌ها چیده شود
 * - کلید fired_{id} در sync_meta جلوی اعلان تکراری همان وقوع را (مثلاً بعد از ریبوت) می‌گیرد
 */
object AlarmScheduler {

    const val ACTION_FIRE = "com.hossein.yademan.action.REMINDER_FIRE"
    const val ACTION_DONE = "com.hossein.yademan.action.REMINDER_DONE"
    const val EXTRA_ID = "reminder_id"
    const val EXTRA_OCCURRENCE = "occurrence_at"
    const val EXTRA_FOLLOW_UP = "follow_up"
    const val TEST_ID = "__yademan_test__"

    private const val FOLLOW_UP_DELAY = 10 * DateUtils.MINUTE

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** آیا اجازه آلارم دقیق داریم؟ (زیر اندروید ۱۲ همیشه بله) */
    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager(context).canScheduleExactAlarms()

    private fun fireIntent(context: Context, id: String, followUp: Boolean): Intent =
        Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_FIRE
            data = Uri.parse("yademan://${if (followUp) "followup" else "reminder"}/${Uri.encode(id)}")
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_FOLLOW_UP, followUp)
        }

    private fun setAlarm(context: Context, triggerAt: Long, pi: PendingIntent) {
        val am = alarmManager(context)
        try {
            if (canScheduleExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (e: SecurityException) {
            // اگر کاربر مجوز آلارم دقیق را در همین لحظه گرفته باشد
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /** زمان‌بندی (یا لغو) آلارم وقوع بعدی یک یادآور */
    suspend fun schedule(context: Context, r: Reminder) {
        cancelMain(context, r.id)
        if (r.isDeleted || !r.alarmEnabled) return
        val now = System.currentTimeMillis()
        var occ = Recurrence.nextOccurrence(r, now) ?: return
        val fired = ServiceLocator.meta.get(MetaKeys.FIRED_PREFIX + r.id)?.toLongOrNull()
        if (fired == occ) {
            occ = Recurrence.nextOccurrence(r, occ + DateUtils.MINUTE) ?: return
        }
        val offset = r.alarmOffsetMinutes.coerceAtLeast(0) * DateUtils.MINUTE
        val trigger = max(occ - offset, now + 1_500)
        val intent = fireIntent(context, r.id, followUp = false).putExtra(EXTRA_OCCURRENCE, occ)
        val pi = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarm(context, trigger, pi)
    }

    /** یادآوری دوباره (حالت «هوشمند»): اگر تا ۱۰ دقیقه بعد از موعد انجام نشد */
    fun scheduleFollowUp(context: Context, id: String, occurrence: Long) {
        val now = System.currentTimeMillis()
        val at = max(occurrence, now) + FOLLOW_UP_DELAY
        val intent = fireIntent(context, id, followUp = true).putExtra(EXTRA_OCCURRENCE, occurrence)
        val pi = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarm(context, at, pi)
    }

    /** اعلان آزمایشی چند ثانیه بعد (برای تست با اپ بسته) */
    fun scheduleTest(context: Context, delaySeconds: Int = 10) {
        val intent = fireIntent(context, TEST_ID, followUp = false)
            .putExtra(EXTRA_OCCURRENCE, System.currentTimeMillis() + delaySeconds * 1000L)
        val pi = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarm(context, System.currentTimeMillis() + delaySeconds * 1000L, pi)
    }

    private fun cancelMain(context: Context, id: String) {
        val pi = PendingIntent.getBroadcast(
            context, 0, fireIntent(context, id, followUp = false),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager(context).cancel(pi)
            pi.cancel()
        }
    }

    fun cancelFollowUp(context: Context, id: String) {
        val pi = PendingIntent.getBroadcast(
            context, 0, fireIntent(context, id, followUp = true),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager(context).cancel(pi)
            pi.cancel()
        }
    }

    /** لغو کامل آلارم‌های یک یادآور */
    fun cancel(context: Context, id: String) {
        cancelMain(context, id)
        cancelFollowUp(context, id)
    }

    /** بازچینش همه آلارم‌ها (استارت اپ، ریبوت، تغییر ساعت، Worker دوره‌ای، بعد از سینک) */
    suspend fun rescheduleAll(context: Context) {
        val all = ServiceLocator.reminders.getAll()
        for (r in all) {
            try {
                schedule(context, r)
            } catch (e: Exception) {
                // یک رکورد خراب نباید بقیه را متوقف کند
            }
        }
    }
}
