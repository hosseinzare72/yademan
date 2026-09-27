// مسیر: app/src/main/java/com/hossein/yademan/YademanApp.kt
package com.hossein.yademan

import android.app.Application
import com.hossein.yademan.data.Seed
import com.hossein.yademan.services.AlarmScheduler
import com.hossein.yademan.services.DailyAlarmRescheduleWorker
import com.hossein.yademan.services.NotificationHelper
import com.hossein.yademan.services.SyncWorker
import kotlinx.coroutines.launch

/**
 * کلاس Application: راه‌اندازی DI، کانال نوتیفیکیشن، seed اولیه،
 * بازچینش آلارم‌ها و زمان‌بندی سینک/Workerها.
 */
class YademanApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        NotificationHelper.createChannels(this)

        ServiceLocator.appScope.launch {
            Seed.seedIfNeeded()
            AlarmScheduler.rescheduleAll(this@YademanApp)
        }

        DailyAlarmRescheduleWorker.schedule(this)
        SyncWorker.schedulePeriodic(this)
        // سینک هنگام استارت (اگر وارد نشده باشد Worker فوراً بی‌کار تمام می‌شود)
        SyncWorker.requestSoon(this, delaySeconds = 2)
    }
}
