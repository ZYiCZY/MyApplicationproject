package com.example.myapplicationproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplicationproject.models.TripLog
import java.util.Locale

// 【準時出門助手】實際耗時紀錄轉接器 (Adapter)
class RecordsAdapter(
    private var logs: List<TripLog>,
    private val onItemClick: (TripLog) -> Unit
) : RecyclerView.Adapter<RecordsAdapter.RecordViewHolder>() {

    class RecordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRecordText: TextView = view.findViewById(R.id.tvRecordText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val log = logs[position]
        val display = String.format(
            Locale.getDefault(),
            "【%s】%s\n準備: %dm | 交通: %dm | 停車步行: %dm (總耗時 %dm)",
            log.tripTitle, log.dateText, log.actualPrepMins, log.actualTravelMins, log.actualLastMileMins, log.actualTotalMins
        )
        holder.tvRecordText.text = display
        holder.itemView.setOnClickListener {
            onItemClick(log)
        }
    }

    override fun getItemCount(): Int = logs.size

    fun updateData(newLogs: List<TripLog>) {
        this.logs = newLogs
        notifyDataSetChanged()
    }
}
