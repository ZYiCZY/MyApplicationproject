package com.example.myapplicationproject.repository

import android.content.Context
import android.content.pm.PackageManager
import com.example.myapplicationproject.services.PlaceInfo
import com.example.myapplicationproject.utils.TimeCalculator
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RouteResult(
    val origin: PlaceInfo,
    val destination: PlaceInfo,
    val transportMode: String, // 汽車, 機車, 步行, 腳踏車, 大眾運輸
    val distanceMeters: Int,
    val durationSeconds: Long,
    val distanceKmText: String,
    val durationMinsText: String,
    val prepStartTimeText: String,
    val departureTimeText: String,
    val arrivalTimeText: String,
    val dataSource: String, // Google Routes API 或 尚未設定 Google 路線服務 (手動備用模式)
    val queryTimestamp: Long = System.currentTimeMillis(),
    val isRealApi: Boolean = false,
    val polylinePoints: List<LatLng> = emptyList()
)

/**
 * 路線查詢儲存庫：負責串接 Google Routes API 與處理交通時間計算
 */
class RouteRepository(private val context: Context) {

    suspend fun computeRoute(
        origin: PlaceInfo,
        destination: PlaceInfo,
        transportMode: String,
        meetingTime: Long,
        prepMins: Int,
        bufferMins: Int
    ): RouteResult = withContext(Dispatchers.IO) {

        // 估算模式 (手動輸入路上分鐘數)
        val baseTravelMins = 0
        val durationSeconds = 0L
        val distanceKmText = "--"
        val durationMinsText = "請手動輸入"

        val departureTime: Long = TimeCalculator.calculateDepartureTime(meetingTime, baseTravelMins, bufferMins)
        val prepStartTime: Long = TimeCalculator.calculatePrepStartTime(departureTime, prepMins)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val prepText = timeFormat.format(Date(prepStartTime))
        val depText = timeFormat.format(Date(departureTime))
        val arrText = timeFormat.format(Date(meetingTime))

        val dataSourceText = "估算值（請手動輸入路上分鐘數）"

        val polyline = listOf(
            origin.latLng,
            LatLng((origin.latLng.latitude + destination.latLng.latitude) / 2, (origin.latLng.longitude + destination.latLng.longitude) / 2),
            destination.latLng
        )

        RouteResult(
            origin = origin,
            destination = destination,
            transportMode = transportMode,
            distanceMeters = 0,
            durationSeconds = durationSeconds,
            distanceKmText = distanceKmText,
            durationMinsText = durationMinsText,
            prepStartTimeText = prepText,
            departureTimeText = depText,
            arrivalTimeText = arrText,
            dataSource = dataSourceText,
            isRealApi = false,
            polylinePoints = polyline
        )
    }
}
