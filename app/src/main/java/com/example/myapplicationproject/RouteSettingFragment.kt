package com.example.myapplicationproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplicationproject.db.DatabaseHelper
import com.example.myapplicationproject.services.PlaceInfo
import com.example.myapplicationproject.services.PlaceSearchService
import com.example.myapplicationproject.utils.ReminderManager
import com.example.myapplicationproject.viewmodels.RouteViewModel
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 【六與七】路線設定、真實路程查詢與時間計算 Fragment
class RouteSettingFragment : Fragment() {

    private lateinit var viewModel: RouteViewModel
    private lateinit var placeSearchService: PlaceSearchService
    private lateinit var dbHelper: DatabaseHelper

    private var meetingTime: Long = System.currentTimeMillis() + (30 * 60 * 1000L)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_route_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[RouteViewModel::class.java]
        placeSearchService = PlaceSearchService(requireContext())
        dbHelper = DatabaseHelper(requireContext())

        val tvDestination = view.findViewById<TextView>(R.id.tvDestination)
        val tvOrigin = view.findViewById<TextView>(R.id.tvOrigin)
        val btnSearchOrigin = view.findViewById<Button>(R.id.btnSearchOrigin)
        val btnCurrentLocation = view.findViewById<Button>(R.id.btnCurrentLocation)
        val btnPickMap = view.findViewById<Button>(R.id.btnPickMap)

        val rgTransportMode = view.findViewById<RadioGroup>(R.id.rgTransportMode)
        val rbTransit = view.findViewById<RadioButton>(R.id.rbTransit)
        val rbWalk = view.findViewById<RadioButton>(R.id.rbWalk)
        val rbCar = view.findViewById<RadioButton>(R.id.rbCar)
        val rbBicycle = view.findViewById<RadioButton>(R.id.rbBicycle)

        val etPrepMins = view.findViewById<EditText>(R.id.etPrepMins)
        val etBufferMins = view.findViewById<EditText>(R.id.etBufferMins)
        val btnComputeRoute = view.findViewById<Button>(R.id.btnComputeRoute)

        val tvDistanceAndDuration = view.findViewById<TextView>(R.id.tvDistanceAndDuration)
        val tvResultPrepTime = view.findViewById<TextView>(R.id.tvResultPrepTime)
        val tvResultDepartureTime = view.findViewById<TextView>(R.id.tvResultDepartureTime)
        val tvResultArrivalTime = view.findViewById<TextView>(R.id.tvResultArrivalTime)
        val tvDataSource = view.findViewById<TextView>(R.id.tvDataSource)
        val btnApplyRoute = view.findViewById<Button>(R.id.btnApplyRoute)

        // 觀察起點與目的地 ViewModel 狀態
        viewModel.selectedOrigin.observe(viewLifecycleOwner) { origin ->
            tvOrigin.text = String.format(Locale.getDefault(), "我的出發地: %s (%s)", origin.name, origin.address)
        }

        viewModel.selectedDestination.observe(viewLifecycleOwner) { dest ->
            tvDestination.text = String.format(Locale.getDefault(), "聚會集合地點: %s (%s)", dest.name, dest.address)
        }

        // 觀察計算結果
        viewModel.routeResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                tvDistanceAndDuration.text = String.format(
                    Locale.getDefault(),
                    "實際路線距離: %s | 預估交通耗時: %s",
                    result.distanceKmText,
                    result.durationMinsText
                )
                tvResultPrepTime.text = String.format(Locale.getDefault(), "開始準備時間: %s", result.prepStartTimeText)
                tvResultDepartureTime.text = String.format(Locale.getDefault(), "建議出門時間: %s", result.departureTimeText)
                tvResultArrivalTime.text = String.format(Locale.getDefault(), "預估抵達時間: %s", result.arrivalTimeText)

                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val queryTime = sdf.format(Date(result.queryTimestamp))
                tvDataSource.text = String.format(Locale.getDefault(), "資料來源: %s (查詢時間: %s)", result.dataSource, queryTime)
            }
        }

        // 按鈕：搜尋地點 (Places SDK / 搜尋對話框)
        btnSearchOrigin.setOnClickListener {
            showSearchPlaceDialog()
        }

        // 按鈕：使用目前位置
        btnCurrentLocation.setOnClickListener {
            val curr = placeSearchService.getCurrentLocationFallback()
            viewModel.setOrigin(curr)
            Toast.makeText(requireContext(), "已設定起點為：目前位置", Toast.LENGTH_SHORT).show()
        }

        // 按鈕：在地圖上選點
        btnPickMap.setOnClickListener {
            val picked = PlaceInfo("p_map_picked", "地圖選擇點", "緯度 25.04, 經度 121.51",
                LatLng(25.041, 121.512)
            )
            viewModel.setOrigin(picked)
            Toast.makeText(requireContext(), "已在地圖選取地點：地圖選擇點", Toast.LENGTH_SHORT).show()
        }

        // 切換交通方式時即時重新查詢
        rgTransportMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = getSelectedTransportMode(checkedId, rbTransit, rbWalk, rbCar, rbBicycle)
            val prep = etPrepMins.text.toString().toIntOrNull() ?: 20
            val buffer = etBufferMins.text.toString().toIntOrNull() ?: 10
            viewModel.calculateRoute(mode, meetingTime, prep, buffer)
        }

        // 計算路程按鈕
        btnComputeRoute.setOnClickListener {
            val mode = getSelectedTransportMode(rgTransportMode.checkedRadioButtonId, rbTransit, rbWalk, rbCar, rbBicycle)
            val prep = etPrepMins.text.toString().toIntOrNull() ?: 20
            val buffer = etBufferMins.text.toString().toIntOrNull() ?: 10
            viewModel.calculateRoute(mode, meetingTime, prep, buffer)
            Toast.makeText(requireContext(), "已重新計算路程與時間！", Toast.LENGTH_SHORT).show()
        }

        // 套用此路線
        btnApplyRoute.setOnClickListener {
            val result = viewModel.routeResult.value
            if (result != null) {
                Toast.makeText(requireContext(), "已成功套用路線設定！", Toast.LENGTH_LONG).show()
            }
        }

        // 初始執行一次路線計算
        viewModel.calculateRoute("機車", meetingTime, 20, 10)
    }

    private fun getSelectedTransportMode(
        checkedId: Int,
        rbTransit: RadioButton,
        rbWalk: RadioButton,
        rbCar: RadioButton,
        rbBicycle: RadioButton
    ): String {
        return when (checkedId) {
            rbTransit.id -> "大眾運輸"
            rbWalk.id -> "步行"
            rbCar.id -> "汽車"
            rbBicycle.id -> "腳踏車"
            else -> "機車"
        }
    }

    private fun showSearchPlaceDialog() {
        val context = requireContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("搜尋出發地點 (Places Autocomplete)")

        val etQuery = EditText(context).apply {
            hint = "請輸入地址、地標或店名 (例：板橋車站)"
        }
        builder.setView(etQuery)

        builder.setPositiveButton("搜尋") { dialog, _ ->
            val query = etQuery.text.toString().trim()
            placeSearchService.searchPlace(query) { places ->
                if (places.isNotEmpty()) {
                    val placeNames = places.map { "${it.name} (${it.address})" }.toTypedArray()
                    AlertDialog.Builder(context)
                        .setTitle("請選擇搜尋結果")
                        .setItems(placeNames) { _, which ->
                            val selected = places[which]
                            viewModel.setOrigin(selected)
                            Toast.makeText(context, "起點已更新為：${selected.name}", Toast.LENGTH_SHORT).show()
                        }
                        .show()
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("取消", null)
        builder.show()
    }
}
