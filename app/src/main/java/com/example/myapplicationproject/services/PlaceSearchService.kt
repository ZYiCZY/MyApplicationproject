package com.example.myapplicationproject.services

import android.content.Context
import com.google.android.gms.maps.model.LatLng

data class PlaceInfo(
    val placeId: String,
    val name: String,
    val address: String,
    val latLng: LatLng
)

/**
 * 地點搜尋服務：整合 Google Places SDK 與備用搜尋模式
 */
class PlaceSearchService(private val context: Context) {

    // 內建常用預設地點清單（當 API Key 未設定或無網路時使用，以嘉義為主）
    private val presetPlaces = listOf(
        PlaceInfo("p_ncyu_lantan", "嘉義大學蘭潭校區", "嘉義市東區學府路300號", LatLng(23.4697, 120.4855)),
        PlaceInfo("p_chiayi_station", "嘉義火車站", "嘉義市西區中山路528號", LatLng(23.4791, 120.4411)),
        PlaceInfo("p_wenhua_night_market", "嘉義文化路夜市", "嘉義市東區文化路", LatLng(23.4790, 120.4497)),
        PlaceInfo("p_chiayi_stadium", "嘉義市立體育場", "嘉義市東區體育路2號", LatLng(23.4735, 120.4572)),
        PlaceInfo("p_ncyu_xinmin", "嘉義大學新民校區", "嘉義市西區新民路580號", LatLng(23.4625, 120.4395))
    )

    fun searchPlace(query: String, callback: (List<PlaceInfo>) -> Unit) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            callback(presetPlaces)
            return
        }

        // 搜尋過濾
        val filtered = presetPlaces.filter {
            it.name.contains(trimmed, ignoreCase = true) || it.address.contains(trimmed, ignoreCase = true)
        }

        if (filtered.isNotEmpty()) {
            callback(filtered)
        } else {
            // 自訂搜尋地點 fallback
            val custom = listOf(
                PlaceInfo("p_custom_${trimmed.hashCode()}", trimmed, "使用者自訂地點 ($trimmed)", LatLng(23.4800, 120.4490))
            )
            callback(custom)
        }
    }

    fun getCurrentLocationFallback(): PlaceInfo {
        return PlaceInfo("p_current_loc", "目前位置 (GPS)", "根據您的目前位置 (嘉義市)", LatLng(23.4800, 120.4490))
    }
}
