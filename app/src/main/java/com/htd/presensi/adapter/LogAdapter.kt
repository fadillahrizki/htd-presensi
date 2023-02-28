package com.htd.presensi.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.htd.presensi.R
import com.htd.presensi.activity.HistoryDetailActivity
import com.htd.presensi.models.Log
import com.htd.presensi.models.Presence
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class LogAdapter(var context: Context) : RecyclerView.Adapter<LogAdapter.MyViewHolder>() {
    var data: ArrayList<Log> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder = MyViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.log, parent, false))

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val dt = data[position]

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        val date =  sdf.parse(dt.date)

        holder.tvDate.text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id")).format(date)
        holder.tvTime.text = SimpleDateFormat("HH:mm").format(date)
        holder.tvText.text = dt.text
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvDate: TextView
        var tvTime: TextView
        var tvText: TextView

        init {
            tvDate = itemView.findViewById(R.id.date)
            tvTime = itemView.findViewById(R.id.time)
            tvText = itemView.findViewById(R.id.text)
        }
    }
}