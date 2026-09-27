// مسیر: app/src/main/java/com/hossein/yademan/utils/DateUtils.kt
package com.hossein.yademan.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * ابزار زمان به وقت محلی دستگاه، بدون java.time (سازگار با minSdk 24 بدون desugaring).
 */
object DateUtils {
    const val MINUTE = 60_000L
    const val HOUR = 60 * MINUTE
    const val DAY = 24 * HOUR

    /** شماره روز محلی از مبدأ یونیکس (برای مقایسه روزها) */
    fun epochDay(millis: Long): Long {
        val offset = TimeZone.getDefault().getOffset(millis)
        return Math.floorDiv(millis + offset, DAY)
    }

    /** ابتدای روز محلی */
    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun todayStart(): Long = startOfDay(System.currentTimeMillis())

    /** افزودن روز با رعایت تغییر ساعت تابستانی */
    fun addDays(millis: Long, days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        add(Calendar.DAY_OF_MONTH, days)
    }.timeInMillis

    /** همان روز، در ساعت و دقیقه مشخص */
    fun atTime(dayMillis: Long, hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        timeInMillis = dayMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun hourOf(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)
    fun minuteOf(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.MINUTE)

    /** کلید روز محلی مثل 2026-09-27 (برای done_dates) */
    fun dayKey(millis: Long): String {
        // بدون ساختن SimpleDateFormat در هر فراخوانی (در حلقه‌های تکرار خیلی پرهزینه بود)
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH) + 1
        val d = c.get(Calendar.DAY_OF_MONTH)
        return buildString(10) {
            append(y)
            append('-')
            if (m < 10) append('0')
            append(m)
            append('-')
            if (d < 10) append('0')
            append(d)
        }
    }

    fun todayKey(): String = dayKey(System.currentTimeMillis())

    /** کش پارس ISO: پارس رشته تاریخ پرهزینه است و همان رشته‌ها بارها در هر صفحه پارس می‌شدند */
    private val isoCache = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private const val NO_VALUE = Long.MIN_VALUE

    fun isoToMillis(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        val cached = isoCache[iso]
        if (cached != null) return if (cached == NO_VALUE) null else cached
        val v = JalaliConverter.parseIso(iso)?.time
        if (isoCache.size > 4000) isoCache.clear()
        isoCache[iso] = v ?: NO_VALUE
        return v
    }

    fun millisToIso(millis: Long): String = JalaliConverter.millisToIso(millis)

    fun nowIso(): String = JalaliConverter.nowUtcIso()

    /** «۰۸:۳۰» */
    fun timeFa(millis: Long): String =
        "%02d:%02d".format(Locale.US, hourOf(millis), minuteOf(millis)).fa()

    /** «۷ مهر» */
    fun dateShortFa(millis: Long): String = JalaliConverter.fromMillis(millis).formatShort()

    /** زمان نسبی فارسی: «۵ دقیقه پیش»، «دیروز»، «۳ روز پیش» */
    fun relativeFa(millis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - millis
        if (diff < 0) {
            val ahead = -diff
            return when {
                ahead < HOUR -> "${(ahead / MINUTE).coerceAtLeast(1).toInt().fa()} دقیقه دیگر"
                ahead < DAY -> "${(ahead / HOUR).toInt().fa()} ساعت دیگر"
                else -> "${(ahead / DAY).toInt().fa()} روز دیگر"
            }
        }
        val dayDiff = epochDay(now) - epochDay(millis)
        return when {
            diff < MINUTE -> "همین الان"
            diff < HOUR -> "${(diff / MINUTE).toInt().fa()} دقیقه پیش"
            dayDiff == 0L -> "${(diff / HOUR).toInt().fa()} ساعت پیش"
            dayDiff == 1L -> "دیروز"
            dayDiff < 7 -> "${dayDiff.toInt().fa()} روز پیش"
            dayDiff < 30 -> "${(dayDiff / 7).toInt().fa()} هفته پیش"
            dayDiff < 365 -> "${(dayDiff / 30).toInt().fa()} ماه پیش"
            else -> "${(dayDiff / 365).toInt().fa()} سال پیش"
        }
    }
}
