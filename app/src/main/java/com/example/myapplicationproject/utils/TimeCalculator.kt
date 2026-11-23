package com.example.myapplicationproject.utils

import com.example.myapplicationproject.models.Trip
import com.example.myapplicationproject.models.TripLog
import java.util.Calendar
import java.util.TimeZone

data class CalculationResult(
    val startPrepTime: Long, // 開始準備時間
    val latestDepartureTime: Long, // 最晚出門時間
    val estimatedArrivalAtStop: Long, // 預估抵達停車/下車地點時間
    val estimatedArrivalAtDestination: Long // 預估到達實際目的地時間
)

data class PersonalizedSuggestion(
    val suggestedPrepMins: Int,
    val suggestedTravelMins: Int,
    val suggestedLastMileMins: Int,
    val sampleCount: Int
)

data class EffectiveTimeValues(
    val prepMins: Int,
    val travelMins: Int,
    val lastMileMins: Int,
    val isPersonalized: Boolean,
    val sampleCount: Int
)

/**
 * 準時出門助手 - 時間計算與個人化數據分析工具
 * 時區：Asia/Taipei，支援跨午夜處理
 */
object TimeCalculator {

    private val timeZone = TimeZone.getTimeZone("Asia/Taipei")

    /**
     * 核心時間計算公式：
     * 最晚出門時間 ＝ 抵達期限 － 交通時間 － 最後一段時間 (停車/步行) － 緩衝時間 － (雨天加算)
     * 開始準備時間 ＝ 最晚出門時間 － 準備時間
     */
    fun calculateTripTimes(
        arrivalDeadline: Long,
        prepMins: Int,
        travelMins: Int,
        lastMileMins: Int,
        bufferMins: Int,
        rainBufferMins: Int = 0,
        isRainyDay: Boolean = false
    ): CalculationResult {
        val totalMarginMins = travelMins + lastMileMins + bufferMins + (if (isRainyDay) rainBufferMins else 0)

        val latestDepartureCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = arrivalDeadline
            add(Calendar.MINUTE, -totalMarginMins)
        }
        val latestDepartureTime = latestDepartureCal.timeInMillis

        val startPrepCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = latestDepartureTime
            add(Calendar.MINUTE, -prepMins)
        }
        val startPrepTime = startPrepCal.timeInMillis

        val arrStopCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = latestDepartureTime
            add(Calendar.MINUTE, travelMins)
        }
        val estimatedArrivalAtStop = arrStopCal.timeInMillis

        val arrDestCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = estimatedArrivalAtStop
            add(Calendar.MINUTE, lastMileMins)
        }
        val estimatedArrivalAtDestination = arrDestCal.timeInMillis

        return CalculationResult(
            startPrepTime = startPrepTime,
            latestDepartureTime = latestDepartureTime,
            estimatedArrivalAtStop = estimatedArrivalAtStop,
            estimatedArrivalAtDestination = estimatedArrivalAtDestination
        )
    }

    /**
     * 共用時間計算與中位數邏輯：若有 5 筆以上完整紀錄則使用中位數，否則使用 Trip 預設值
     */
    fun getEffectiveTimeValues(trip: Trip, logs: List<TripLog>): EffectiveTimeValues {
        val completeLogs = logs.filter { it.tripId == trip.id && it.isComplete && it.actualPrepMins > 0 }
        if (completeLogs.size >= 5) {
            val suggestion = calculatePersonalizedSuggestion(completeLogs)
            if (suggestion != null) {
                return EffectiveTimeValues(
                    prepMins = suggestion.suggestedPrepMins,
                    travelMins = suggestion.suggestedTravelMins,
                    lastMileMins = suggestion.suggestedLastMileMins,
                    isPersonalized = true,
                    sampleCount = suggestion.sampleCount
                )
            }
        }
        return EffectiveTimeValues(
            prepMins = trip.prepMins,
            travelMins = trip.travelMins,
            lastMileMins = trip.lastMileMins,
            isPersonalized = false,
            sampleCount = completeLogs.size
        )
    }

    fun getEffectiveTripTimes(
        trip: Trip,
        logs: List<TripLog>,
        arrivalDeadline: Long = trip.arrivalDeadline
    ): CalculationResult {
        val values = getEffectiveTimeValues(trip, logs)
        return calculateTripTimes(
            arrivalDeadline = arrivalDeadline,
            prepMins = values.prepMins,
            travelMins = values.travelMins,
            lastMileMins = values.lastMileMins,
            bufferMins = trip.bufferMins,
            rainBufferMins = trip.rainBufferMins,
            isRainyDay = trip.isRainyDay
        )
    }

    /**
     * 保持向下相容的兩段計算
     */
    fun calculateDepartureTime(meetingTime: Long, travelMins: Int, bufferMins: Int): Long {
        return calculateTripTimes(meetingTime, 0, travelMins, 0, bufferMins).latestDepartureTime
    }

    fun calculatePrepStartTime(departureTime: Long, prepMins: Int): Long {
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = departureTime
            add(Calendar.MINUTE, -prepMins)
        }
        return calendar.timeInMillis
    }

    /**
     * 根據過去累積 5+ 筆實際耗時紀錄，使用中位數 (Median) 提供個人化估算建議
     */
    fun calculatePersonalizedSuggestion(logs: List<TripLog>): PersonalizedSuggestion? {
        val completeLogs = logs.filter { it.isComplete && it.actualPrepMins > 0 }
        if (completeLogs.size < 5) return null

        val prepList = completeLogs.map { it.actualPrepMins }.sorted()
        val travelList = completeLogs.map { it.actualTravelMins }.sorted()
        val lastMileList = completeLogs.map { it.actualLastMileMins }.sorted()

        val medianPrep = getMedian(prepList)
        val medianTravel = getMedian(travelList)
        val medianLastMile = getMedian(lastMileList)

        return PersonalizedSuggestion(
            suggestedPrepMins = medianPrep,
            suggestedTravelMins = medianTravel,
            suggestedLastMileMins = medianLastMile,
            sampleCount = completeLogs.size
        )
    }

    private fun getMedian(sortedList: List<Int>): Int {
        val size = sortedList.size
        return if (size % 2 == 1) {
            sortedList[size / 2]
        } else {
            (sortedList[size / 2 - 1] + sortedList[size / 2]) / 2
        }
    }
}
