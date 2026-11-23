package com.example.myapplicationproject.models

/**
 * 準時出門助手 - 行程資料模型
 */
data class Trip(
    val id: String = "",
    val title: String = "", // 例如：早八資管課、送小宇上學+上班
    val destinationName: String = "嘉義大學蘭潭校區", // 目的地名稱 (例：嘉義大學蘭潭校區)
    val destinationAddress: String = "嘉義市東區學府路300號",
    val latitude: Double = 23.4697,
    val longitude: Double = 120.4855,
    val arrivalDeadline: Long = 0L, // 抵達期限 (epoch millis, 例如 08:00)
    val transportMode: String = "機車", // 機車, 大眾運輸, 步行, 汽車, 腳踏車
    val prepMins: Int = 20, // 準備時間
    val travelMins: Int = 8, // 交通時間
    val lastMileMins: Int = 10, // 最後一段時間 (停車、下車後走到教室/辦公室)
    val bufferMins: Int = 5, // 緩衝時間
    val rainBufferMins: Int = 10, // 雨天額外加算時間
    val isRainyDay: Boolean = false,
    val mustBringItems: String = "安全帽,鑰匙,學生證", // 必帶清單 (最多3項，逗號分隔)
    val remindEveningBefore: Boolean = true, // 前一晚提醒
    val remindStartPrep: Boolean = true, // 開始準備提醒
    val remindDeparture: Boolean = true, // 最晚出門提醒
    val isMultiStop: Boolean = false, // 是否為多站行程 (例：住處->幼兒園->公司)
    val stopsSummary: String = "", // 多站說明
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 實際耗時紀錄資料模型 (4階段時間戳記)
 */
data class TripLog(
    val id: String = "",
    val tripId: String = "",
    val tripTitle: String = "",
    val dateText: String = "",
    val startPrepTime: Long? = null, // 1. 開始準備時間
    val leftHomeTime: Long? = null, // 2. 已出門時間
    val parkedTime: Long? = null, // 3. 已停車/下車時間
    val arrivedTime: Long? = null, // 4. 已到達實際目的地時間
    val actualPrepMins: Int = 0, // 實際準備耗時
    val actualTravelMins: Int = 0, // 實際交通耗時
    val actualLastMileMins: Int = 0, // 實際最後一段耗時
    val actualTotalMins: Int = 0, // 實際總耗時
    val isComplete: Boolean = false
)
