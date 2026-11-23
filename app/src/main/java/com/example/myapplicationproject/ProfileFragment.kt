package com.example.myapplicationproject

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplicationproject.db.DatabaseHelper
import com.example.myapplicationproject.receiver.ReminderReceiver
import com.example.myapplicationproject.utils.TripScheduler
import com.example.myapplicationproject.viewmodels.ProfileViewModel
import java.util.Locale

// 【準時出門助手】個人設定、預設交通方式修改、勿擾模式檢查與通知測試
class ProfileFragment : Fragment() {

    private lateinit var viewModel: ProfileViewModel
    private var defaultTransportMode: String = "機車"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        val tvNickname = view.findViewById<TextView>(R.id.tvNickname)
        val tvPrepTimeSetting = view.findViewById<TextView>(R.id.tvPrepTimeSetting)
        val tvLastMileSetting = view.findViewById<TextView>(R.id.tvLastMileSetting)
        val tvTransportSetting = view.findViewById<TextView>(R.id.tvTransportSetting)

        val etNickname = view.findViewById<EditText>(R.id.etNickname)
        val etPrepTimeSetting = view.findViewById<EditText>(R.id.etPrepTimeSetting)
        val etLastMileSetting = view.findViewById<EditText>(R.id.etLastMileSetting)
        val btnSaveSettings = view.findViewById<Button>(R.id.btnSaveSettings)

        val tvNotificationDndStatus = view.findViewById<TextView>(R.id.tvNotificationDndStatus)
        val btnTestVibration = view.findViewById<Button>(R.id.btnTestVibration)
        val btnOpenSystemSettings = view.findViewById<Button>(R.id.btnOpenSystemSettings)
        val btnTestEveningReminder = view.findViewById<Button>(R.id.btnTestEveningReminder)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedPrepMins = prefs.getInt("pref_default_prep_mins", 20)
        val savedLastMileMins = prefs.getInt("pref_default_last_mile_mins", 10)

        tvPrepTimeSetting.text = String.format(Locale.getDefault(), "預設準備時間: %d 分鐘", savedPrepMins)
        tvLastMileSetting.text = String.format(Locale.getDefault(), "預設最後一哩 (停車步行): %d 分鐘", savedLastMileMins)

        viewModel.nickname.observe(viewLifecycleOwner) { nickname ->
            tvNickname.text = String.format(Locale.getDefault(), "暱稱: %s", nickname)
        }

        viewModel.defaultPrepMins.observe(viewLifecycleOwner) { mins ->
            tvPrepTimeSetting.text = String.format(Locale.getDefault(), "預設準備時間: %d 分鐘", mins)
        }

        // 修改預設交通方式（單純修改個人預設偏好）
        tvTransportSetting.setOnClickListener {
            val modes = arrayOf("機車", "大眾運輸", "步行", "汽車", "腳踏車")
            AlertDialog.Builder(requireContext())
                .setTitle("選擇預設個人交通方式")
                .setItems(modes) { _, which ->
                    defaultTransportMode = modes[which]
                    tvTransportSetting.text = String.format(Locale.getDefault(), "預設交通方式: %s (點擊修改)", defaultTransportMode)
                    Toast.makeText(requireContext(), "預設交通方式已更新為：$defaultTransportMode", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        checkNotificationAndDndStatus(tvNotificationDndStatus)

        // 測試通知與震動
        btnTestVibration.setOnClickListener {
            val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
                putExtra("EXTRA_TRIP_ID", "test_vibration")
                putExtra("EXTRA_TITLE", "通知與震動測試 (準時出門助手)")
                putExtra("EXTRA_MESSAGE", "若您看到此訊息並感受到短震動，說明提醒功能正常發揮！")
            }
            requireContext().sendBroadcast(intent)
            Toast.makeText(requireContext(), "已發送測試通知！請檢查通知欄與震動", Toast.LENGTH_SHORT).show()
        }

        // 開啟系統設定
        btnOpenSystemSettings.setOnClickListener {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                }
            }
            startActivity(intent)
        }

        // 測試：1 分鐘後發送前一晚提醒按鈕 (獨立 requestCode 1999, EXTRA_IS_TEST = true)
        btnTestEveningReminder.setOnClickListener {
            val dbHelper = DatabaseHelper(requireContext())
            val trips = dbHelper.getTrips()
            if (trips.isEmpty()) {
                Toast.makeText(requireContext(), "請先在「今天」分頁設定行程", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val trip = trips.first()
            val itemsText = trip.mustBringItems.trim()
            val msg = if (itemsText.isNotBlank()) {
                String.format(Locale.getDefault(), "明天的行程：%s。記得帶：%s", trip.destinationName, itemsText.replace(",", "、"))
            } else {
                String.format(Locale.getDefault(), "明天的行程：%s", trip.destinationName)
            }

            val triggerTime = System.currentTimeMillis() + (60 * 1000L)

            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
                putExtra("EXTRA_TRIP_ID", trip.id)
                putExtra("EXTRA_TITLE", "前一晚必帶物品提醒 (測試)")
                putExtra("EXTRA_MESSAGE", msg)
                putExtra("EXTRA_IS_TEST", true)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                TripScheduler.REQUEST_CODE_TEST,
                intent,
                flags
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }

            Toast.makeText(requireContext(), "已成功排程 1 分鐘後發送測試提醒！", Toast.LENGTH_LONG).show()
        }

        btnSaveSettings.setOnClickListener {
            val newNickname = etNickname.text.toString().trim()
            val newPrepMinsText = etPrepTimeSetting.text.toString().trim()
            val newLastMileText = etLastMileSetting.text.toString().trim()

            if (newNickname.isNotBlank()) {
                viewModel.updateNickname(newNickname)
            }

            val editor = prefs.edit()

            if (newPrepMinsText.isNotBlank()) {
                val mins = newPrepMinsText.toIntOrNull()
                if (mins != null && mins > 0) {
                    viewModel.updateDefaultPrepMins(mins)
                    editor.putInt("pref_default_prep_mins", mins)
                    tvPrepTimeSetting.text = String.format(Locale.getDefault(), "預設準備時間: %d 分鐘", mins)
                }
            }

            if (newLastMileText.isNotBlank()) {
                val lastMile = newLastMileText.toIntOrNull()
                if (lastMile != null && lastMile > 0) {
                    editor.putInt("pref_default_last_mile_mins", lastMile)
                    tvLastMileSetting.text = String.format(Locale.getDefault(), "預設最後一哩 (停車步行): %d 分鐘", lastMile)
                }
            }

            editor.apply()
            Toast.makeText(requireContext(), "個人設定已更新！", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("登出帳號")
                .setMessage("確定要登出目前帳號嗎？")
                .setPositiveButton("登出") { _, _ ->
                    Toast.makeText(requireContext(), "已成功登出！", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<TextView>(R.id.tvNotificationDndStatus)?.let {
            checkNotificationAndDndStatus(it)
        }
    }

    private fun checkNotificationAndDndStatus(tvStatus: TextView) {
        val context = requireContext()
        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val dndFilter = notificationManager.currentInterruptionFilter
        val isDndActive = dndFilter != NotificationManager.INTERRUPTION_FILTER_ALL

        val notifText = if (notificationsEnabled) "已開啟" else "已關閉 (提醒受限)"
        val dndText = if (isDndActive) "已開啟 (提醒可能受靜音影響)" else "未開啟"

        tvStatus.text = String.format(Locale.getDefault(), "系統通知權限: %s\n勿擾模式狀態: %s", notifText, dndText)
    }
}
