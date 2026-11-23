package com.example.myapplicationproject.utils

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import com.example.myapplicationproject.db.DatabaseHelper
import com.example.myapplicationproject.models.Trip
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object TripScheduler {

    const val REQUEST_CODE_EVENING = 1001
    const val REQUEST_CODE_DEPARTURE = 1002
    const val REQUEST_CODE_TEST = 1999

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleTripReminders(context: Context, trip: Trip) {
        val dbHelper = DatabaseHelper(context)
        val logs = dbHelper.getTripLogsByTripId(trip.id)

        val tz = TimeZone.getTimeZone("Asia/Taipei")
        val now = System.currentTimeMillis()

        val calDeadline = Calendar.getInstance(tz).apply {
            timeInMillis = trip.arrivalDeadline
        }
        val hour = calDeadline.get(Calendar.HOUR_OF_DAY)
        val minute = calDeadline.get(Calendar.MINUTE)

        // 計算當天對應的抵達時間
        val targetArrivalCal = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 計算當天的出門時間
        var calc = TimeCalculator.getEffectiveTripTimes(trip, logs, targetArrivalCal.timeInMillis)

        // 若當前的出門時間已過，自動換算為明天的抵達時間與出門時間
        if (now >= calc.latestDepartureTime) {
            targetArrivalCal.add(Calendar.DAY_OF_YEAR, 1)
            calc = TimeCalculator.getEffectiveTripTimes(trip, logs, targetArrivalCal.timeInMillis)
        }

        val departureTime = calc.latestDepartureTime

        // 計算該次行程前一天的 21:00 提醒時間
        val eveningCal = (targetArrivalCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val eveningMillis = eveningCal.timeInMillis

        // 取消先前的舊提醒
        cancelTripReminders(context)

        // 1. 前一晚 21:00 必帶物品提醒 (若未過期則排程)
        if (eveningMillis > now) {
            val itemsText = trip.mustBringItems.trim()
            val messageStr = if (itemsText.isNotBlank()) {
                String.format(Locale.getDefault(), "明天的行程：%s。記得帶：%s", trip.destinationName, itemsText.replace(",", "、"))
            } else {
                String.format(Locale.getDefault(), "明天的行程：%s", trip.destinationName)
            }
            ReminderManager.scheduleReminder(
                context = context,
                tripId = trip.id,
                title = "前一晚必帶物品提醒",
                message = messageStr,
                triggerTime = eveningMillis,
                requestCode = REQUEST_CODE_EVENING
            )
        }

        // 2. 出門時間提醒 (若未過期則排程)
        if (departureTime > now) {
            ReminderManager.scheduleReminder(
                context = context,
                tripId = trip.id,
                title = "出門提醒 (準時出門助手)",
                message = "現在出門",
                triggerTime = departureTime,
                requestCode = REQUEST_CODE_DEPARTURE
            )
        }
    }

    fun cancelTripReminders(context: Context) {
        ReminderManager.cancelReminder(context, REQUEST_CODE_EVENING)
        ReminderManager.cancelReminder(context, REQUEST_CODE_DEPARTURE)
    }
}
