package com.example.myapplicationproject.receiver

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.myapplicationproject.db.DatabaseHelper
import com.example.myapplicationproject.utils.TripScheduler
import java.util.concurrent.Executors

// 【準時出門助手】背景提醒接收器
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "ontime_alerts"
    }

    private val executor = Executors.newSingleThreadExecutor()

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "準時出門助手提醒"
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "您預設的行程提醒時間已到！"
        val tripId = intent.getStringExtra("EXTRA_TRIP_ID") ?: ""
        val isTest = intent.getBooleanExtra("EXTRA_IS_TEST", false)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "準時出門提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "用於發送前一晚必帶物品與出門提醒"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setAutoCancel(true)

        val notificationId = if (tripId.isNotEmpty()) tripId.hashCode() else System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, builder.build())

        // 若不是測試提醒，於背景執行續中重新向資料庫讀取並重新排程下一次行程
        if (!isTest) {
            val pendingResult = goAsync()
            executor.execute {
                try {
                    val dbHelper = DatabaseHelper(context)
                    val trips = dbHelper.getTrips()
                    if (trips.isNotEmpty()) {
                        val trip = trips.first()
                        TripScheduler.scheduleTripReminders(context, trip)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
