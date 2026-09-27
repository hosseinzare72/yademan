// مسیر: app/src/main/java/com/hossein/yademan/data/ReminderMeta.kt
package com.hossein.yademan.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hossein.yademan.utils.DateUtils

/** انواع یادآور و نگاشت آن‌ها به دسته پیش‌فرض */
object ReminderType {
    const val INSTALLMENT = "installment"
    const val CHECK = "check"
    const val SUBSCRIPTION = "subscription"
    const val MEETING = "meeting"
    const val MEDICINE = "medicine"
    const val SHOPPING = "shopping"
    const val NOTE = "note"
    const val OTHER = "other"

    const val CAT_INSTALLMENT = "cat_installment"
    const val CAT_CHECK = "cat_check"
    const val CAT_SUBSCRIPTION = "cat_subscription"
    const val CAT_MEETING = "cat_meeting"
    const val CAT_MEDICINE = "cat_medicine"
    const val CAT_SHOPPING = "cat_shopping"
    const val CAT_NOTE = "cat_note"

    fun categoryId(type: String): String? = when (type) {
        INSTALLMENT -> CAT_INSTALLMENT
        CHECK -> CAT_CHECK
        SUBSCRIPTION -> CAT_SUBSCRIPTION
        MEETING -> CAT_MEETING
        MEDICINE -> CAT_MEDICINE
        SHOPPING -> CAT_SHOPPING
        NOTE -> CAT_NOTE
        else -> null
    }

    fun fromCategory(categoryId: String?): String = when (categoryId) {
        CAT_INSTALLMENT -> INSTALLMENT
        CAT_CHECK -> CHECK
        CAT_SUBSCRIPTION -> SUBSCRIPTION
        CAT_MEETING -> MEETING
        CAT_MEDICINE -> MEDICINE
        CAT_SHOPPING -> SHOPPING
        CAT_NOTE -> NOTE
        else -> OTHER
    }

    /** رنگ هگز هر نوع (از colors.json مخزن) */
    fun colorHex(type: String): String = when (type) {
        INSTALLMENT -> "#E8B84B"
        CHECK -> "#9D8CFF"
        SUBSCRIPTION -> "#FF8F5C"
        MEETING -> "#4CC3FF"
        MEDICINE -> "#FF7A9E"
        SHOPPING -> "#3ED598"
        NOTE -> "#FFB84D"
        else -> "#D9B45B"
    }

    fun labelFa(type: String): String = when (type) {
        INSTALLMENT -> "قسط"
        CHECK -> "چک"
        SUBSCRIPTION -> "اشتراک"
        MEETING -> "قرار"
        MEDICINE -> "دارو"
        SHOPPING -> "خرید"
        NOTE -> "یادداشت"
        else -> "سایر"
    }
}

/** یک مورد چک‌لیست یادداشت */
data class NoteItem(val text: String, val done: Boolean)

/**
 * خواندن/نوشتن meta_json.
 *
 * ساختار قراردادی meta برای هر نوع:
 * - همه:          {"type": "installment|check|meeting|medicine|shopping|note|other", "done_dates": ["2026-09-27", ...]}
 *                  done_dates فقط برای یادآورهای تکراری است (انجام/مصرف/پرداخت در آن روز). یک‌بارها از is_done استفاده می‌کنند.
 * - installment/check: {"bank": "بانک ملت", "check_number": "۱۲۳۴", "total_count": 10}
 *                  total_count (اختیاری، برای همه تکراری‌ها): بعد از این تعداد وقوع، تکرار و هشدار تمام می‌شود
 * - subscription: {"plan": "فیلیمو • خانوادگی", "total_count"?: 12}   ← مبلغ در amount_toman، دوره = repeat_rule
 * - meeting:      {"location": "دفتر مرکزی"}
 * - medicine:     {"dose": "۵۰۰ میلی‌گرم"}   ← «مصرف امروز» = done_dates شامل کلید امروز
 * - shopping:     {"group": "لبنیات", "qty": "۲ بطری"}   ← قیمت در amount_toman
 * - note:         {"pinned": true, "items": [{"text": "...", "done": false}]}   ← متن آزاد در note
 */
object ReminderMeta {

    private const val MAX_DONE_DATES = 120

    /** نسخه تازه و قابل تغییر (برای نوشتن) */
    fun parse(json: String?): JsonObject = try {
        val el = JsonParser.parseString(json ?: "{}")
        if (el.isJsonObject) el.asJsonObject else JsonObject()
    } catch (e: Exception) {
        JsonObject()
    }

    // ---------- کش خواندن ----------
    // meta_json هر رکورد در هر رندر صفحه ده‌ها بار پارس می‌شد (عامل اصلی لگ). حالا هر رشته فقط یک‌بار پارس می‌شود.
    private val readCache = java.util.concurrent.ConcurrentHashMap<String, JsonObject>()
    private val doneCache = java.util.concurrent.ConcurrentHashMap<String, Set<String>>()
    private const val CACHE_LIMIT = 3000

    /** فقط خواندنی؛ هرگز شیء برگشتی را تغییر ندهید */
    private fun read(json: String?): JsonObject {
        val key = json ?: "{}"
        readCache[key]?.let { return it }
        val o = parse(key)
        if (readCache.size > CACHE_LIMIT) readCache.clear()
        readCache[key] = o
        return o
    }

    fun type(r: Reminder): String =
        str(r, "type")?.takeIf { it.isNotBlank() } ?: ReminderType.fromCategory(r.categoryId)

    fun str(r: Reminder, key: String): String? {
        val el = read(r.metaJson).get(key) ?: return null
        return if (el.isJsonPrimitive) el.asString else null
    }

    fun bool(r: Reminder, key: String): Boolean {
        val el = read(r.metaJson).get(key) ?: return false
        return try {
            el.isJsonPrimitive && el.asBoolean
        } catch (e: Exception) {
            false
        }
    }

    /** مقدار یک کلید را تنظیم می‌کند (null = حذف) و رکورد جدید برمی‌گرداند */
    fun with(r: Reminder, key: String, value: Any?): Reminder {
        val o = parse(r.metaJson)
        when (value) {
            null -> o.remove(key)
            is String -> o.addProperty(key, value)
            is Boolean -> o.addProperty(key, value)
            is Number -> o.addProperty(key, value)
            else -> o.addProperty(key, value.toString())
        }
        return r.copy(metaJson = o.toString())
    }

    fun int(r: Reminder, key: String): Int? {
        val el = read(r.metaJson).get(key) ?: return null
        return try {
            if (el.isJsonPrimitive) el.asString.trim().toIntOrNull() else null
        } catch (e: Exception) {
            null
        }
    }

    fun doneDates(r: Reminder): Set<String> {
        doneCache[r.metaJson]?.let { return it }
        val arr = read(r.metaJson).get("done_dates")
        val set: Set<String> = if (arr == null || !arr.isJsonArray) emptySet()
        else arr.asJsonArray.mapNotNull { if (it.isJsonPrimitive) it.asString else null }.toHashSet()
        if (doneCache.size > CACHE_LIMIT) doneCache.clear()
        doneCache[r.metaJson] = set
        return set
    }

    /** تعداد کل دفعات تکرار (مثلاً ۱۰ قسط)؛ null = نامحدود */
    fun totalCount(r: Reminder): Int? = int(r, "total_count")?.takeIf { it > 0 }

    /** آیا وقوعِ روزِ dayKey انجام/مصرف/پرداخت شده؟ */
    fun isDoneOn(r: Reminder, dayKey: String): Boolean =
        if (r.repeatRule == "none" || r.repeatRule.isBlank()) r.isDone else dayKey in doneDates(r)

    /** علامت‌گذاری انجام برای یک روز (تکراری‌ها) یا کل یادآور (یک‌بارها) */
    fun withDoneOn(r: Reminder, dayKey: String, done: Boolean): Reminder {
        if (r.repeatRule == "none" || r.repeatRule.isBlank()) {
            return r.copy(isDone = done, doneAt = if (done) DateUtils.nowIso() else null)
        }
        val set = doneDates(r).toMutableSet()
        if (done) set.add(dayKey) else set.remove(dayKey)
        val trimmed = set.sorted().takeLast(MAX_DONE_DATES)
        val o = parse(r.metaJson)
        val arr = JsonArray()
        trimmed.forEach { arr.add(it) }
        o.add("done_dates", arr)
        return r.copy(metaJson = o.toString())
    }

    fun noteItems(r: Reminder): List<NoteItem> {
        val arr = read(r.metaJson).get("items")
        if (arr == null || !arr.isJsonArray) return emptyList()
        return arr.asJsonArray.mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            val o = el.asJsonObject
            val text = o.get("text")?.takeIf { it.isJsonPrimitive }?.asString ?: return@mapNotNull null
            val done = try {
                o.get("done")?.asBoolean ?: false
            } catch (e: Exception) {
                false
            }
            NoteItem(text, done)
        }
    }

    fun withNoteItems(r: Reminder, items: List<NoteItem>): Reminder {
        val o = parse(r.metaJson)
        val arr = JsonArray()
        items.forEach { item ->
            arr.add(JsonObject().apply {
                addProperty("text", item.text)
                addProperty("done", item.done)
            })
        }
        o.add("items", arr)
        return r.copy(metaJson = o.toString())
    }

    /** ساخت meta اولیه برای یک نوع */
    fun build(type: String, extras: Map<String, Any?> = emptyMap()): String {
        val o = JsonObject()
        o.addProperty("type", type)
        extras.forEach { (k, v) ->
            when (v) {
                null -> Unit
                is String -> if (v.isNotBlank()) o.addProperty(k, v)
                is Boolean -> o.addProperty(k, v)
                is Number -> o.addProperty(k, v)
                is List<*> -> {
                    val arr = JsonArray()
                    v.forEach { item -> if (item is String) arr.add(item) }
                    o.add(k, arr)
                }
                else -> o.addProperty(k, v.toString())
            }
        }
        return o.toString()
    }
}
