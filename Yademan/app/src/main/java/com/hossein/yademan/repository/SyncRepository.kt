// مسیر: app/src/main/java/com/hossein/yademan/repository/SyncRepository.kt
package com.hossein.yademan.repository

import com.hossein.yademan.data.SyncMeta
import com.hossein.yademan.data.SyncMetaDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** کلیدهای sync_meta */
object MetaKeys {
    const val SEEDED = "seeded_v1"
    const val LAST_SYNC = "last_sync"              // مقدار برگشتی سرور
    const val LAST_SYNC_LOCAL = "last_sync_local"  // زمان محلی آخرین سینک موفق (برای نمایش)
    const val LAST_SYNC_ERROR = "last_sync_error"
    const val NOTIF_SMART = "notif_smart"
    const val NOTIF_SOUND = "notif_sound"
    const val NOTIF_DND = "notif_dnd"
    const val DISPLAY_NAME = "display_name"
    const val NOTIF_PERMISSION_ASKED = "notif_permission_asked"
    const val FIRED_PREFIX = "fired_"              // آخرین وقوعی که برایش اعلان داده شده
}

/** دسترسی key-value به sync_meta (متادیتای سینک + تنظیمات) */
class SyncRepository(private val dao: SyncMetaDao) {
    suspend fun get(key: String): String? = dao.get(key)
    suspend fun set(key: String, value: String) = dao.set(SyncMeta(key, value))
    suspend fun delete(key: String) = dao.delete(key)
    fun observe(key: String): Flow<String?> = dao.observe(key).distinctUntilChanged()

    suspend fun getBool(key: String, default: Boolean): Boolean =
        dao.get(key)?.toBooleanStrictOrNull() ?: default

    suspend fun setBool(key: String, value: Boolean) = set(key, value.toString())

    fun observeBool(key: String, default: Boolean): Flow<Boolean> =
        dao.observe(key).map { it?.toBooleanStrictOrNull() ?: default }.distinctUntilChanged()
}
