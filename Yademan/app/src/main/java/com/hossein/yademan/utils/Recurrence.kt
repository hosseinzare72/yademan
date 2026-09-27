// مسیر: app/src/main/java/com/hossein/yademan/utils/Recurrence.kt
package com.hossein.yademan.utils

import com.hossein.yademan.data.Reminder
import com.hossein.yademan.data.ReminderMeta
import kotlin.math.max

/**
 * موتور تکرار یادآورها.
 * - daily: هر N روز
 * - weekly: هر N هفته (اگر weekdays_mask تنظیم شده باشد فقط روزهای انتخابی؛ بیت ۰=شنبه ... ۶=جمعه)
 * - monthly: هر N ماه «شمسی» در همان روز ماه (اگر ماه کوتاه‌تر بود، آخرین روز ماه)
 * - yearly: هر N سال شمسی در همان ماه و روز
 * - none: فقط یک‌بار در due_at
 * ساعت وقوع همیشه ساعت/دقیقه due_at به وقت محلی است.
 *
 * v1.2:
 * - تعداد دفعات محدود: meta_json.total_count (مثلاً ۱۰ قسط). end_at هم هنگام ذخیره روی پایان روزِ
 *   آخرین وقوع تنظیم می‌شود، پس بعد از آخرین قسط هیچ وقوع/آلارمی ساخته نمی‌شود.
 * - بهینه‌سازی: base/end/done_dates فقط یک‌بار محاسبه می‌شوند، ماهانه/سالانه ماه‌به‌ماه جلو می‌رود
 *   (نه روزبه‌روز)، و بعد از end_at جست‌وجو فوراً متوقف می‌شود.
 */
object Recurrence {

    private const val SEARCH_DAYS = 800
    private const val SEARCH_MONTHS = 600

    const val NONE = "none"
    const val DAILY = "daily"
    const val WEEKLY = "weekly"
    const val MONTHLY = "monthly"
    const val YEARLY = "yearly"

    fun baseMillis(r: Reminder): Long? = DateUtils.isoToMillis(r.dueAt)

    fun isRepeating(r: Reminder): Boolean = r.repeatRule != NONE && r.repeatRule.isNotBlank()

    /** سقف زمانی تکرار: end_at صریح یا پایان روز آخرین وقوع (اگر total_count داریم) */
    private fun endMillis(r: Reminder, base: Long): Long? {
        val explicit = DateUtils.isoToMillis(r.endAt)
        val count = ReminderMeta.totalCount(r)
        if (count == null || !isRepeating(r)) return explicit
        val last = nthOccurrenceRaw(r, base, count) ?: return explicit
        val lastDayEnd = DateUtils.addDays(DateUtils.startOfDay(last), 1) - 1
        return if (explicit == null) lastDayEnd else minOf(explicit, lastDayEnd)
    }

    /** اگر یادآور در روزِ داده‌شده وقوع دارد، زمان دقیق آن را برمی‌گرداند؛ وگرنه null */
    fun occurrenceOnDay(r: Reminder, dayMillis: Long): Long? {
        val base = baseMillis(r) ?: return null
        return occurrenceOnDay(r, dayMillis, base, endMillis(r, base))
    }

    private fun occurrenceOnDay(r: Reminder, dayMillis: Long, base: Long, end: Long?): Long? {
        val baseDay = DateUtils.epochDay(base)
        val day = DateUtils.epochDay(dayMillis)
        if (day < baseDay) return null
        if (end != null && dayMillis > end) return null
        val interval = r.repeatInterval.coerceAtLeast(1)
        val matches = when (r.repeatRule) {
            DAILY -> (day - baseDay) % interval == 0L
            WEEKLY -> {
                if (r.weekdaysMask != 0) {
                    val wd = JalaliConverter.fromMillis(dayMillis).weekday
                    val weeks = (day - baseDay) / 7
                    (r.weekdaysMask and (1 shl wd)) != 0 && weeks % interval == 0L
                } else {
                    (day - baseDay) % (7L * interval) == 0L
                }
            }
            MONTHLY -> {
                val jb = JalaliConverter.fromMillis(base)
                val jt = JalaliConverter.fromMillis(dayMillis)
                val months = (jt.year - jb.year) * 12 + (jt.month - jb.month)
                val targetDay = minOf(jb.day, JalaliConverter.monthLength(jt.year, jt.month))
                months >= 0 && months % interval == 0 && jt.day == targetDay
            }
            YEARLY -> {
                val jb = JalaliConverter.fromMillis(base)
                val jt = JalaliConverter.fromMillis(dayMillis)
                val years = jt.year - jb.year
                val targetDay = minOf(jb.day, JalaliConverter.monthLength(jt.year, jt.month))
                years >= 0 && years % interval == 0 && jt.month == jb.month && jt.day == targetDay
            }
            else -> day == baseDay
        }
        if (!matches) return null
        val occ = DateUtils.atTime(dayMillis, DateUtils.hourOf(base), DateUtils.minuteOf(base))
        if (end != null && occ > end) return null
        return occ
    }

    /** وقوع k-ام ماهانه/سالانه (k از ۰) — مستقیم، بدون حلقه روزانه */
    private fun calendarOccurrence(r: Reminder, base: Long, k: Int): Long {
        val jb = JalaliConverter.fromMillis(base)
        val interval = r.repeatInterval.coerceAtLeast(1)
        val monthsToAdd = if (r.repeatRule == YEARLY) 12 * interval * k else interval * k
        val n = JalaliConverter.normalizeMonth(jb.year, jb.month + monthsToAdd)
        val day = minOf(jb.day, JalaliConverter.monthLength(n[0], n[1]))
        return JalaliConverter.toMillis(n[0], n[1], day, DateUtils.hourOf(base), DateUtils.minuteOf(base))
    }

    /** وقوع n-ام (n از ۱) بدون در نظر گرفتن end — برای محاسبه آخرین قسط */
    private fun nthOccurrenceRaw(r: Reminder, base: Long, n: Int): Long? {
        if (n <= 0) return null
        val interval = r.repeatInterval.coerceAtLeast(1)
        return when (r.repeatRule) {
            MONTHLY, YEARLY -> calendarOccurrence(r, base, n - 1)
            DAILY -> DateUtils.addDays(base, (n - 1) * interval)
            WEEKLY -> if (r.weekdaysMask == 0) DateUtils.addDays(base, (n - 1) * 7 * interval) else {
                var count = 0
                var day = DateUtils.startOfDay(base)
                repeat(SEARCH_DAYS * 4) {
                    val occ = occurrenceOnDay(r, day, base, null)
                    if (occ != null) {
                        count++
                        if (count == n) return occ
                    }
                    day = DateUtils.addDays(day, 1)
                }
                null
            }
            else -> if (n == 1) base else null
        }
    }

    /** وقوع n-ام (n از ۱) — آخرین قسط = nthOccurrence(r, total_count) */
    fun nthOccurrence(r: Reminder, n: Int): Long? {
        val base = baseMillis(r) ?: return null
        return nthOccurrenceRaw(r, base, n)
    }

    /** آخرین وقوع برای یادآورهای با تعداد محدود؛ وگرنه null */
    fun lastOccurrence(r: Reminder): Long? {
        val count = ReminderMeta.totalCount(r) ?: return null
        if (!isRepeating(r)) return null
        return nthOccurrence(r, count)
    }

    /** شماره این وقوع (۱ = اولین) — برای نمایش «قسط ۳ از ۱۰» */
    fun occurrenceIndex(r: Reminder, occ: Long): Int? {
        val base = baseMillis(r) ?: return null
        if (occ < DateUtils.startOfDay(base)) return null
        val interval = r.repeatInterval.coerceAtLeast(1)
        val days = DateUtils.epochDay(occ) - DateUtils.epochDay(base)
        return when (r.repeatRule) {
            DAILY -> (days / interval).toInt() + 1
            WEEKLY -> if (r.weekdaysMask == 0) (days / (7L * interval)).toInt() + 1 else {
                var count = 0
                var day = DateUtils.startOfDay(base)
                val target = DateUtils.epochDay(occ)
                repeat(SEARCH_DAYS * 4) {
                    if (DateUtils.epochDay(day) > target) return count
                    if (occurrenceOnDay(r, day, base, null) != null) count++
                    day = DateUtils.addDays(day, 1)
                }
                count
            }
            MONTHLY, YEARLY -> {
                val jb = JalaliConverter.fromMillis(base)
                val jt = JalaliConverter.fromMillis(occ)
                val months = (jt.year - jb.year) * 12 + (jt.month - jb.month)
                val step = if (r.repeatRule == YEARLY) 12 * interval else interval
                months / step + 1
            }
            else -> 1
        }
    }

    /**
     * اولین وقوع در زمان fromMillis یا بعد از آن.
     * اگر skipDone=true باشد، وقوع‌هایی که انجام‌شده علامت خورده‌اند رد می‌شوند.
     */
    fun nextOccurrence(r: Reminder, fromMillis: Long, skipDone: Boolean = true): Long? {
        val base = baseMillis(r) ?: return null
        if (!isRepeating(r)) {
            if (skipDone && r.isDone) return null
            return if (base >= fromMillis) base else null
        }
        val end = endMillis(r, base)
        if (end != null && fromMillis > end) return null
        val done = if (skipDone) ReminderMeta.doneDates(r) else emptySet()
        fun ok(occ: Long): Boolean = occ >= fromMillis && (end == null || occ <= end) &&
            !(skipDone && DateUtils.dayKey(occ) in done)

        // ماهانه/سالانه: ماه‌به‌ماه (سریع)
        if (r.repeatRule == MONTHLY || r.repeatRule == YEARLY) {
            val jb = JalaliConverter.fromMillis(base)
            val jf = JalaliConverter.fromMillis(max(base, fromMillis))
            val monthsDiff = (jf.year - jb.year) * 12 + (jf.month - jb.month)
            val step = (if (r.repeatRule == YEARLY) 12 else 1) * r.repeatInterval.coerceAtLeast(1)
            var k = max(0, monthsDiff / step - 1)
            repeat(SEARCH_MONTHS) {
                val occ = calendarOccurrence(r, base, k)
                if (end != null && occ > end) return null
                if (ok(occ)) return occ
                k++
            }
            return null
        }

        // روزانه/هفتگی: روزبه‌روز با توقف فوری بعد از end
        var day = DateUtils.startOfDay(max(base, fromMillis))
        repeat(SEARCH_DAYS) {
            if (end != null && day > end) return null
            val occ = occurrenceOnDay(r, day, base, end)
            if (occ != null && ok(occ)) return occ
            day = DateUtils.addDays(day, 1)
        }
        return null
    }

    /** متن فارسی قانون تکرار (+ تعداد دفعات اگر محدود است) */
    fun labelFa(r: Reminder): String {
        val n = r.repeatInterval.coerceAtLeast(1)
        val base = when (r.repeatRule) {
            DAILY -> if (n == 1) "هر روز" else "هر ${n.fa()} روز"
            WEEKLY -> if (n == 1) "هر هفته" else "هر ${n.fa()} هفته"
            MONTHLY -> if (n == 1) "هر ماه" else "هر ${n.fa()} ماه"
            YEARLY -> if (n == 1) "هر سال" else "هر ${n.fa()} سال"
            else -> return "یک‌بار"
        }
        val count = ReminderMeta.totalCount(r) ?: return base
        return "$base • ${count.fa()} بار"
    }
}
