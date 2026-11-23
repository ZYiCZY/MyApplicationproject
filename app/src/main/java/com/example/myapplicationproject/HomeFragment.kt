package com.example.myapplicationproject

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplicationproject.db.DatabaseHelper
import com.example.myapplicationproject.models.Trip
import com.example.myapplicationproject.models.TripLog
import com.example.myapplicationproject.utils.TimeCalculator
import com.example.myapplicationproject.utils.TripScheduler
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

// 【準時出門助手】首頁 - 行程倒推時間計算 (M1/M2)、必帶清單 (M5)、4階段打卡與ETA分享
class HomeFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private var currentTrip: Trip? = null

    private var selectedHour: Int = 8
    private var selectedMinute: Int = 0

    // 4階段打卡時間紀錄點
    private var logStartPrepTime: Long? = null
    private var logLeftHomeTime: Long? = null
    private var logParkedTime: Long? = null
    private var logArrivedTime: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())

        val etDestinationName = view.findViewById<EditText>(R.id.etDestinationName)
        val btnPickArrivalTime = view.findViewById<Button>(R.id.btnPickArrivalTime)
        val tvSelectedArrivalTime = view.findViewById<TextView>(R.id.tvSelectedArrivalTime)
        val etTravelMins = view.findViewById<EditText>(R.id.etTravelMins)
        val spinnerTransportMode = view.findViewById<Spinner>(R.id.spinnerTransportMode)
        val etMustBringItems = view.findViewById<EditText>(R.id.etMustBringItems)
        val btnSaveTrip = view.findViewById<Button>(R.id.btnSaveTrip)

        val tvMeetingName = view.findViewById<TextView>(R.id.tvMeetingName)
        val tvMeetingLocation = view.findViewById<TextView>(R.id.tvMeetingLocation)
        val tvPersonalizedLabel = view.findViewById<TextView>(R.id.tvPersonalizedLabel)
        val tvPrepTime = view.findViewById<TextView>(R.id.tvPrepTime)
        val tvLeaveTime = view.findViewById<TextView>(R.id.tvLeaveTime)
        val tvEstimatedArrival = view.findViewById<TextView>(R.id.tvEstimatedArrival)
        val tvLastActualDuration = view.findViewById<TextView>(R.id.tvLastActualDuration)
        val tvTimeBreakdown = view.findViewById<TextView>(R.id.tvTimeBreakdown)

        val cbItem1 = view.findViewById<CheckBox>(R.id.cbItem1)
        val cbItem2 = view.findViewById<CheckBox>(R.id.cbItem2)
        val cbItem3 = view.findViewById<CheckBox>(R.id.cbItem3)

        val switchRainyDay = view.findViewById<Switch>(R.id.switchRainyDay)

        val btnStep1Prep = view.findViewById<Button>(R.id.btnStep1Prep)
        val btnStep2Leave = view.findViewById<Button>(R.id.btnStep2Leave)
        val btnStep3Park = view.findViewById<Button>(R.id.btnStep3Park)
        val btnStep4Arrive = view.findViewById<Button>(R.id.btnStep4Arrive)

        val btnShareEta = view.findViewById<Button>(R.id.btnShareEta)
        val btnRouteSetting = view.findViewById<Button>(R.id.btnRouteSetting)
        btnRouteSetting.visibility = View.GONE

        // 設定交通方式 Spinner 選項
        val transportModes = arrayOf("機車", "大眾運輸", "步行", "汽車", "腳踏車")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, transportModes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTransportMode.adapter = adapter

        // 抵達時間選擇 Dialog
        btnPickArrivalTime.setOnClickListener {
            val timePicker = TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    selectedHour = hourOfDay
                    selectedMinute = minute
                    tvSelectedArrivalTime.text = String.format(Locale.getDefault(), "抵達時間: %02d:%02d", hourOfDay, minute)
                },
                selectedHour,
                selectedMinute,
                true
            )
            timePicker.show()
        }

        // 載入資料庫現有行程
        loadTripData(
            etDestinationName, tvSelectedArrivalTime, etTravelMins, spinnerTransportMode,
            etMustBringItems, tvMeetingName, tvMeetingLocation, tvPersonalizedLabel, tvPrepTime,
            tvLeaveTime, tvEstimatedArrival, tvLastActualDuration, tvTimeBreakdown,
            cbItem1, cbItem2, cbItem3, switchRainyDay
        )

        // 儲存 / 更新單一行程 (M1 + M5 + 排程)
        btnSaveTrip.setOnClickListener {
            val destName = etDestinationName.text.toString().trim()
            if (destName.isEmpty()) {
                Toast.makeText(requireContext(), "請輸入目的地名稱", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val travelMins = etTravelMins.text.toString().toIntOrNull() ?: 15
            val mode = spinnerTransportMode.selectedItem.toString()
            val isRainy = switchRainyDay.isChecked

            // 處理必帶物品 (最多3項，逗號分隔；留空存空字串)
            val rawMustBring = etMustBringItems.text.toString().trim()
            val formattedMustBring = if (rawMustBring.isNotBlank()) {
                rawMustBring.split(",", "，")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .take(3)
                    .joinToString(",")
            } else {
                ""
            }

            val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val defaultPrep = prefs.getInt("pref_default_prep_mins", 20)
            val defaultLastMile = prefs.getInt("pref_default_last_mile_mins", 10)

            val nextArrivalDeadline = calculateNextOccurrenceMillis(selectedHour, selectedMinute)

            lifecycleScope.launch(Dispatchers.IO) {
                val existingTrips = dbHelper.getTrips()
                val tripId = if (existingTrips.isNotEmpty()) existingTrips.first().id else UUID.randomUUID().toString()

                val tripToSave = Trip(
                    id = tripId,
                    title = destName,
                    destinationName = destName,
                    destinationAddress = "嘉義市",
                    latitude = 23.4697,
                    longitude = 120.4855,
                    arrivalDeadline = nextArrivalDeadline,
                    transportMode = mode,
                    prepMins = defaultPrep,
                    travelMins = travelMins,
                    lastMileMins = defaultLastMile,
                    bufferMins = 5,
                    isRainyDay = isRainy,
                    mustBringItems = formattedMustBring,
                    createdAt = System.currentTimeMillis()
                )

                dbHelper.insertTrip(tripToSave)
                TripScheduler.scheduleTripReminders(requireContext(), tripToSave)

                withContext(Dispatchers.Main) {
                    currentTrip = tripToSave

                    // 更新必帶物品 CheckBox
                    updateMustBringCheckBoxes(formattedMustBring, cbItem1, cbItem2, cbItem3)

                    // 更新 UI 時間與中位數標記
                    updateTripDisplay(
                        tvMeetingName, tvMeetingLocation, tvPersonalizedLabel, tvPrepTime,
                        tvLeaveTime, tvEstimatedArrival, tvLastActualDuration, tvTimeBreakdown
                    )

                    // 檢查精確鬧鐘權限
                    if (!TripScheduler.canScheduleExactAlarms(requireContext())) {
                        showExactAlarmPermissionDialog()
                    }

                    Toast.makeText(requireContext(), "行程設定與提醒排程已儲存！", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 1. 雨天加算切換
        switchRainyDay.setOnCheckedChangeListener { _, isChecked ->
            currentTrip?.let { trip ->
                val newTrip = trip.copy(isRainyDay = isChecked)
                currentTrip = newTrip
                dbHelper.updateTripRainyMode(trip.id, isChecked)

                updateTripDisplay(
                    tvMeetingName, tvMeetingLocation, tvPersonalizedLabel, tvPrepTime,
                    tvLeaveTime, tvEstimatedArrival, tvLastActualDuration, tvTimeBreakdown
                )

                // 重新更新提醒時間
                TripScheduler.scheduleTripReminders(requireContext(), newTrip)

                val msg = if (isChecked) "已開啟雨天模式 (+10分鐘緩衝)" else "已關閉雨天模式"
                Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
            }
        }

        // 2. 4階段實際耗時打卡紀錄 (嚴格驗證，不可造假)
        btnStep1Prep.setOnClickListener {
            if (currentTrip == null) {
                Toast.makeText(requireContext(), "請先設定行程才能打卡！", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            logStartPrepTime = System.currentTimeMillis()
            btnStep1Prep.isEnabled = false
            btnStep1Prep.text = "1. 已開始準備"
            Snackbar.make(view, "已開始準備！", Snackbar.LENGTH_SHORT).show()
        }

        btnStep2Leave.setOnClickListener {
            if (currentTrip == null) {
                Toast.makeText(requireContext(), "請先設定行程才能打卡！", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            logLeftHomeTime = System.currentTimeMillis()
            btnStep2Leave.isEnabled = false
            btnStep2Leave.text = "2. 已出門出發"
            Snackbar.make(view, "已出門出發！", Snackbar.LENGTH_SHORT).show()
        }

        btnStep3Park.setOnClickListener {
            if (currentTrip == null) {
                Toast.makeText(requireContext(), "請先設定行程才能打卡！", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            logParkedTime = System.currentTimeMillis()
            btnStep3Park.isEnabled = false
            btnStep3Park.text = "3. 已停車下車"
            Snackbar.make(view, "已到達停車/下車地點！", Snackbar.LENGTH_SHORT).show()
        }

        btnStep4Arrive.setOnClickListener {
            val trip = currentTrip
            if (trip == null) {
                Toast.makeText(requireContext(), "請先設定行程才能打卡！", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val now = System.currentTimeMillis()
            logArrivedTime = now

            val isComplete = (logStartPrepTime != null && logLeftHomeTime != null && logParkedTime != null)
            val sdfDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            val dateStr = sdfDate.format(Date(now))

            if (isComplete) {
                val prepStart = logStartPrepTime!!
                val leftHome = logLeftHomeTime!!
                val parked = logParkedTime!!

                val prepMins = ((leftHome - prepStart) / (60 * 1000L)).toInt().coerceAtLeast(1)
                val travelMins = ((parked - leftHome) / (60 * 1000L)).toInt().coerceAtLeast(1)
                val lastMileMins = ((now - parked) / (60 * 1000L)).toInt().coerceAtLeast(1)
                val totalMins = prepMins + travelMins + lastMileMins

                val tripLog = TripLog(
                    id = UUID.randomUUID().toString(),
                    tripId = trip.id,
                    tripTitle = trip.destinationName,
                    dateText = dateStr,
                    startPrepTime = prepStart,
                    leftHomeTime = leftHome,
                    parkedTime = parked,
                    arrivedTime = now,
                    actualPrepMins = prepMins,
                    actualTravelMins = travelMins,
                    actualLastMileMins = lastMileMins,
                    actualTotalMins = totalMins,
                    isComplete = true
                )

                dbHelper.insertTripLog(tripLog)

                val alertMsg = String.format(
                    Locale.getDefault(),
                    "到達目的地！本次實際耗時記錄已儲存：\n• 準備: %d 分鐘\n• 路上: %d 分鐘\n• 最後一哩: %d 分鐘\n總計: %d 分鐘",
                    prepMins, travelMins, lastMileMins, totalMins
                )

                AlertDialog.Builder(requireContext())
                    .setTitle("行程完成耗時統計")
                    .setMessage(alertMsg)
                    .setPositiveButton("確定", null)
                    .show()
            } else {
                val incompleteLog = TripLog(
                    id = UUID.randomUUID().toString(),
                    tripId = trip.id,
                    tripTitle = trip.destinationName,
                    dateText = dateStr,
                    startPrepTime = logStartPrepTime,
                    leftHomeTime = logLeftHomeTime,
                    parkedTime = logParkedTime,
                    arrivedTime = now,
                    actualPrepMins = 0,
                    actualTravelMins = 0,
                    actualLastMileMins = 0,
                    actualTotalMins = 0,
                    isComplete = false
                )

                dbHelper.insertTripLog(incompleteLog)

                AlertDialog.Builder(requireContext())
                    .setTitle("打卡紀錄未完成")
                    .setMessage("因為有跳過打卡步驟，本次打卡標記為未完成紀錄，不寫入實際耗時統計。")
                    .setPositiveButton("確定", null)
                    .show()
            }

            // 重設打卡按鈕狀態
            resetCheckInButtons(btnStep1Prep, btnStep2Leave, btnStep3Park, btnStep4Arrive)

            // 更新 UI 顯示
            updateTripDisplay(
                tvMeetingName, tvMeetingLocation, tvPersonalizedLabel, tvPrepTime,
                tvLeaveTime, tvEstimatedArrival, tvLastActualDuration, tvTimeBreakdown
            )
        }

        // 3. 預計抵達訊息分享
        btnShareEta.setOnClickListener {
            val trip = currentTrip
            if (trip != null) {
                val logs = dbHelper.getTripLogsByTripId(trip.id)
                val calc = TimeCalculator.getEffectiveTripTimes(trip, logs)
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val arrivalText = sdf.format(Date(calc.estimatedArrivalAtDestination))

                val shareMsg = "【準時出門助手】我已準備出發前往「${trip.destinationName}」，預估將於 $arrivalText 抵達！"

                AlertDialog.Builder(requireContext())
                    .setTitle("預覽預計抵達訊息")
                    .setMessage("訊息內容：\n$shareMsg\n\n確認後將開啟系統分享介面傳送給公司、老師或朋友。")
                    .setPositiveButton("分享訊息") { _, _ ->
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareMsg)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "選擇分享應用程式 (LINE/簡訊)")
                        startActivity(shareIntent)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            } else {
                Toast.makeText(requireContext(), "請先設定行程", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showExactAlarmPermissionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("需要鬧鐘與提醒權限")
            .setMessage("「準時出門助手」需要「鬧鐘與提醒」權限，才能在最晚出門時間與前一晚發送行程提醒。請在系統設定中開啟權限。")
            .setPositiveButton("前往設定") { _, _ ->
                try {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${requireContext().packageName}")
                        }
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${requireContext().packageName}")
                        }
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${requireContext().packageName}")
                    }
                    startActivity(intent)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateMustBringCheckBoxes(
        mustBringItems: String,
        cb1: CheckBox,
        cb2: CheckBox,
        cb3: CheckBox
    ) {
        val items = mustBringItems.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (items.isNotEmpty()) {
            cb1.visibility = View.VISIBLE
            cb1.text = items[0]
            if (items.size >= 2) {
                cb2.visibility = View.VISIBLE
                cb2.text = items[1]
            } else {
                cb2.visibility = View.GONE
            }
            if (items.size >= 3) {
                cb3.visibility = View.VISIBLE
                cb3.text = items[2]
            } else {
                cb3.visibility = View.GONE
            }
        } else {
            cb1.visibility = View.VISIBLE
            cb1.text = "無前一晚必帶物品"
            cb2.visibility = View.GONE
            cb3.visibility = View.GONE
        }
    }

    private fun resetCheckInButtons(
        b1: Button, b2: Button, b3: Button, b4: Button
    ) {
        logStartPrepTime = null
        logLeftHomeTime = null
        logParkedTime = null
        logArrivedTime = null

        b1.isEnabled = true
        b1.text = "1. 開始準備"
        b2.isEnabled = true
        b2.text = "2. 我出門了"
        b3.isEnabled = true
        b3.text = "3. 已停車下車"
        b4.isEnabled = true
        b4.text = "4. 到達目的地"
    }

    private fun calculateNextOccurrenceMillis(hour: Int, minute: Int): Long {
        val tz = TimeZone.getTimeZone("Asia/Taipei")
        val now = Calendar.getInstance(tz)
        val target = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    private fun loadTripData(
        etDestinationName: EditText,
        tvSelectedArrivalTime: TextView,
        etTravelMins: EditText,
        spinnerTransportMode: Spinner,
        etMustBringItems: EditText,
        tvMeetingName: TextView,
        tvMeetingLocation: TextView,
        tvPersonalizedLabel: TextView,
        tvPrepTime: TextView,
        tvLeaveTime: TextView,
        tvEstimatedArrival: TextView,
        tvLastActualDuration: TextView,
        tvTimeBreakdown: TextView,
        cbItem1: CheckBox,
        cbItem2: CheckBox,
        cbItem3: CheckBox,
        switchRainyDay: Switch
    ) {
        val trips = dbHelper.getTrips()
        if (trips.isNotEmpty()) {
            val trip = trips.first()
            currentTrip = trip

            etDestinationName.setText(trip.destinationName)
            etTravelMins.setText(trip.travelMins.toString())
            etMustBringItems.setText(trip.mustBringItems)

            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Taipei")).apply {
                timeInMillis = trip.arrivalDeadline
            }
            selectedHour = cal.get(Calendar.HOUR_OF_DAY)
            selectedMinute = cal.get(Calendar.MINUTE)
            tvSelectedArrivalTime.text = String.format(Locale.getDefault(), "抵達時間: %02d:%02d", selectedHour, selectedMinute)

            val modes = arrayOf("機車", "大眾運輸", "步行", "汽車", "腳踏車")
            val index = modes.indexOf(trip.transportMode).coerceAtLeast(0)
            spinnerTransportMode.setSelection(index)

            switchRainyDay.isChecked = trip.isRainyDay

            updateMustBringCheckBoxes(trip.mustBringItems, cbItem1, cbItem2, cbItem3)

            updateTripDisplay(
                tvMeetingName, tvMeetingLocation, tvPersonalizedLabel, tvPrepTime,
                tvLeaveTime, tvEstimatedArrival, tvLastActualDuration, tvTimeBreakdown
            )
        } else {
            currentTrip = null
            tvMeetingName.text = "尚無行程"
            tvMeetingLocation.text = "請於上方輸入目的地與時間並點擊「儲存 / 更新行程」"
            tvPrepTime.text = "起身時間: --:--"
            tvLeaveTime.text = "出門時間: --:--"
            tvEstimatedArrival.text = "預估抵達時間: --:--"
            tvTimeBreakdown.text = "時間拆解: 尚無資料"
            tvPersonalizedLabel.visibility = View.GONE
            tvLastActualDuration.visibility = View.GONE
            cbItem1.text = "無前一晚必帶物品"
            cbItem2.visibility = View.GONE
            cbItem3.visibility = View.GONE
        }
    }

    private fun updateTripDisplay(
        tvMeetingName: TextView,
        tvMeetingLocation: TextView,
        tvPersonalizedLabel: TextView,
        tvPrepTime: TextView,
        tvLeaveTime: TextView,
        tvEstimatedArrival: TextView,
        tvLastActualDuration: TextView,
        tvTimeBreakdown: TextView
    ) {
        val trip = currentTrip ?: return

        // 限定僅計算目前行程 (trip.id) 的完整紀錄
        val tripLogs = dbHelper.getTripLogsByTripId(trip.id)
        val completeLogs = tripLogs.filter { it.isComplete }

        // 1. 上次實際耗時
        if (completeLogs.isNotEmpty()) {
            val latest = completeLogs.first()
            tvLastActualDuration.visibility = View.VISIBLE
            tvLastActualDuration.text = String.format(Locale.getDefault(), "上次實際花了 %d 分鐘", latest.actualTotalMins)
        } else {
            tvLastActualDuration.visibility = View.GONE
        }

        // 2. 共用中位數與時間計算
        val effectiveValues = TimeCalculator.getEffectiveTimeValues(trip, tripLogs)
        if (effectiveValues.isPersonalized) {
            tvPersonalizedLabel.visibility = View.VISIBLE
            tvPersonalizedLabel.text = String.format(Locale.getDefault(), "依你最近 %d 次的實際紀錄", effectiveValues.sampleCount)
        } else {
            tvPersonalizedLabel.visibility = View.GONE
        }

        val calc = TimeCalculator.getEffectiveTripTimes(trip, tripLogs)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        tvMeetingName.text = trip.destinationName
        tvMeetingLocation.text = String.format(
            Locale.getDefault(),
            "目的地: %s (抵達期限: %s)",
            trip.destinationName,
            timeFormat.format(Date(trip.arrivalDeadline))
        )

        tvPrepTime.text = String.format(Locale.getDefault(), "起身時間: %s", timeFormat.format(Date(calc.startPrepTime)))
        tvLeaveTime.text = String.format(Locale.getDefault(), "出門時間: %s", timeFormat.format(Date(calc.latestDepartureTime)))
        tvEstimatedArrival.text = String.format(Locale.getDefault(), "預估抵達時間: %s", timeFormat.format(Date(calc.estimatedArrivalAtDestination)))

        val rainExtra = if (trip.isRainyDay) " + 雨天${trip.rainBufferMins}m" else ""
        tvTimeBreakdown.text = String.format(
            Locale.getDefault(),
            "時間拆解: 準備 %dm | 路上 %dm | 最後一哩 %dm | 緩衝 %dm%s",
            effectiveValues.prepMins, effectiveValues.travelMins, effectiveValues.lastMileMins, trip.bufferMins, rainExtra
        )
    }
}
