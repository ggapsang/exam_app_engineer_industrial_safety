package com.daim.safetyexam.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.util.Calendar

/** F11: 사용자가 지정한 시간에 매일 학습 알림. */
object Reminder {

    private const val CHANNEL_ID = "study_reminder"
    private const val REQ_CODE = 1001
    const val NOTI_ID = 2001

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java)
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "학습 알림", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "지정한 시간에 학습을 알려줍니다."
                }
            )
        }
    }

    /** "HH:mm" 형식 */
    fun schedule(context: Context, time: String) {
        val parts = time.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        ensureChannel(context)
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP, cal.timeInMillis,
            AlarmManager.INTERVAL_DAY, pi
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, StudyReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, REQ_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun notifyNow(context: Context) {
        ensureChannel(context)
        val noti = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("오늘의 산업안전기사 학습")
            .setContentText("오답노트와 모의고사로 약점을 줄여보세요.")
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTI_ID, noti)
    }
}

class StudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminder.notifyNow(context)
    }
}
