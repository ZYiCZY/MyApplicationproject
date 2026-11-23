package com.example.myapplicationproject.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplicationproject.db.DatabaseHelper
import com.example.myapplicationproject.utils.TripScheduler
import java.util.concurrent.Executors

class BootReceiver : BroadcastReceiver() {

    private val executor = Executors.newSingleThreadExecutor()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
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
