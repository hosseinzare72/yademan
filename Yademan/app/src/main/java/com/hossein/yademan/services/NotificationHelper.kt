// مسیر: app/src/main/java/com/hossein/yademan/services/NotificationHelper.kt
package com.hossein.yademan.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hossein.yademan.MainActivity
import com.hossein.yademan.R

/** کانال‌ها و ساخت نوتیفیکیشن */
object NotificationHelper {

    /** کانال اصلی با صدا (importance HIGH) */
    const val CHANNEL_MAIN = "yademan_reminders"
    /** کانال بی‌صدا برای حالت «صدا خاموش» یا «مزاحم نشو» */
    const val CHANNEL_SILENT = "yademan_reminders_silent"

    const val EXTRA_OPEN_ROUTE = "open_route"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val main = NotificationChannel(CHANNEL_MAIN, "یادآوری‌ها", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "اعلان یادآورها، اقساط، داروها و قرارها"
            enableVibration(true)
            enableLights(true)
            lightColor = 0xFFDDAF4B.toInt()
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        val silent = NotificationChannel(CHANNEL_SILENT, "یادآوری‌های بی‌صدا", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "وقتی صدا خاموش است یا حالت مزاحم نشو فعال است"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(main)
        nm.createNotificationChannel(silent)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun areEnabled(context: Context): Boolean =
        hasPermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * نمایش نوتیفیکیشن. doneLabel اگر null نباشد دکمه «انجام شد/مصرف شد/پرداخت شد» اضافه می‌شود.
     * @return true اگر نمایش داده شد
     */
    @SuppressLint("MissingPermission")
    fun show(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        silent: Boolean,
        reminderId: String?,
        occurrence: Long,
        doneLabel: String?
    ): Boolean {
        createChannels(context)
        if (!hasPermission(context)) return false

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_ROUTE, "notifications")
        }
        val contentPi = PendingIntent.getActivity(
            context, notificationId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, if (silent) CHANNEL_SILENT else CHANNEL_MAIN)
            .setSmallIcon(R.drawable.ic_stat_bell)
            .setColor(0xFFDDAF4B.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setContentIntent(contentPi)

        if (silent) {
            builder.setSilent(true)
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        }

        if (reminderId != null && doneLabel != null) {
            val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = AlarmScheduler.ACTION_DONE
                data = Uri.parse("yademan://done/${Uri.encode(reminderId)}")
                putExtra(AlarmScheduler.EXTRA_ID, reminderId)
                putExtra(AlarmScheduler.EXTRA_OCCURRENCE, occurrence)
            }
            val donePi = PendingIntent.getBroadcast(
                context, 0, doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, doneLabel, donePi)
        }

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun cancel(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
