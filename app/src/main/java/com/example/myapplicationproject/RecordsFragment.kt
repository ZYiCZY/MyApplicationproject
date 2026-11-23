package com.example.myapplicationproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplicationproject.db.DatabaseHelper
import com.example.myapplicationproject.models.TripLog
import com.example.myapplicationproject.utils.PersonalizedSuggestion
import com.example.myapplicationproject.utils.TimeCalculator
import java.util.Locale

// 【準時出門助手】歷史耗時紀錄、4階段統計與個人化中位數建議
class RecordsFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvStats: TextView
    private lateinit var tvSuggestionHeader: TextView
    private lateinit var tvSuggestionDetails: TextView
    private lateinit var btnApplySuggestion: Button
    private lateinit var layoutSuggestionCard: LinearLayout

    private lateinit var rvRecords: RecyclerView
    private var activeSuggestion: PersonalizedSuggestion? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_records, container, false)
        tvStats = view.findViewById(R.id.tvStats)
        layoutSuggestionCard = view.findViewById(R.id.layoutSuggestionCard)
        tvSuggestionHeader = view.findViewById(R.id.tvSuggestionHeader)
        tvSuggestionDetails = view.findViewById(R.id.tvSuggestionDetails)
        btnApplySuggestion = view.findViewById(R.id.btnApplySuggestion)
        rvRecords = view.findViewById(R.id.rvRecords)

        dbHelper = DatabaseHelper(requireContext())
        rvRecords.layoutManager = LinearLayoutManager(requireContext())

        return view
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val currentTrip = dbHelper.getTrips().firstOrNull()
        val logs = if (currentTrip != null) {
            dbHelper.getTripLogsByTripId(currentTrip.id)
        } else {
            dbHelper.getTripLogs()
        }

        val completeLogs = logs.filter { it.isComplete }
        val count = completeLogs.size

        tvStats.text = String.format(Locale.getDefault(), "累積出門紀錄: %d 次 (包含準備/路上/最後一哩階段)", count)

        // 計算個人化中位數建議 (需 5+ 筆完整紀錄)
        val suggestion = TimeCalculator.calculatePersonalizedSuggestion(completeLogs)
        activeSuggestion = suggestion

        if (suggestion != null) {
            layoutSuggestionCard.visibility = View.VISIBLE
            btnApplySuggestion.isEnabled = true
            tvSuggestionHeader.text = String.format(Locale.getDefault(), "依你最近 %d 次的紀錄 (中位數)", suggestion.sampleCount)
            tvSuggestionDetails.text = String.format(
                Locale.getDefault(),
                "分析您過去 %d 次真實紀錄（中位數）：\n• 建議準備時間: %d 分鐘\n• 建議路上時間: %d 分鐘\n• 建議最後一哩: %d 分鐘",
                suggestion.sampleCount, suggestion.suggestedPrepMins, suggestion.suggestedTravelMins, suggestion.suggestedLastMileMins
            )
            btnApplySuggestion.setOnClickListener {
                if (currentTrip != null) {
                    dbHelper.updateTripTimeSettings(
                        currentTrip.id,
                        suggestion.suggestedPrepMins,
                        suggestion.suggestedTravelMins,
                        suggestion.suggestedLastMileMins
                    )
                    Toast.makeText(requireContext(), "已成功套用中位數建議至目前行程！", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "尚無行程可套用建議", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            layoutSuggestionCard.visibility = View.VISIBLE
            tvSuggestionHeader.text = "依你最近的紀錄 (尚需累積資料)"
            tvSuggestionDetails.text = String.format(
                Locale.getDefault(),
                "目前已累積 %d/5 次完整打卡紀錄。再記錄 %d 次即可解鎖個人化中位數建議！",
                count,
                (5 - count).coerceAtLeast(1)
            )
            btnApplySuggestion.isEnabled = false
        }

        val adapter = RecordsAdapter(logs) { log ->
            showLogDetailDialog(log)
        }
        rvRecords.adapter = adapter
    }

    private fun showLogDetailDialog(log: TripLog) {
        val context = requireContext()
        val statusText = if (log.isComplete) "完整紀錄" else "未完成 (跳過步驟)"
        val msg = String.format(
            Locale.getDefault(),
            "行程名稱: %s\n日期: %s\n狀態: %s\n\n各階段實際耗時拆解：\n• 1. 準備時間: %d 分鐘\n• 2. 路上時間: %d 分鐘\n• 3. 最後一哩: %d 分鐘\n總計耗時: %d 分鐘",
            log.tripTitle, log.dateText, statusText, log.actualPrepMins, log.actualTravelMins, log.actualLastMileMins, log.actualTotalMins
        )

        AlertDialog.Builder(context)
            .setTitle("耗時紀錄詳細資訊")
            .setMessage(msg)
            .setPositiveButton("確定", null)
            .show()
    }
}
