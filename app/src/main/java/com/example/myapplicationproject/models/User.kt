package com.example.myapplicationproject.models

/**
 * 使用者資料模型
 * 支援 SQLite 與 Firestore（提供預設值以產生無參數建構子）
 */
data class User(
    val id: String = "",
    val email: String = "",
    val nickname: String = "",
    val defaultPrepTime: Int = 20,
    val defaultBufferTime: Int = 10,
    val defaultTransportMode: String = "機車"
)
