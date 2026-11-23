package com.example.myapplicationproject.models

/**
 * 聚會/活動資料模型
 */
data class Event(
    val id: String = "",
    val groupId: String = "",
    val name: String = "",
    val description: String = "",
    val locationName: String = "",
    val latitude: Double = 23.480, // 嘉義市中心座標
    val longitude: Double = 120.449,
    val meetingTime: Long = 0L,
    val gracePeriodMins: Int = 10,
    val isCancelled: Boolean = false
)
