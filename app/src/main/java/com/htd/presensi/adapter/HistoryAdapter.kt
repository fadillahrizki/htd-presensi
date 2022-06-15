package com.htd.presensi.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.htd.presensi.R
import com.htd.presensi.models.Presence

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.MyViewHolder>() {
    var data: ArrayList<Presence> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder = MyViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.presence, parent, false))

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val dt = data[position]
        holder.tvStatus.text = dt.status?.capitalize()
        holder.tvType.text = dt.type?.capitalize()
        holder.tvDate.text = dt.date
        holder.tvTime.text = dt.time

        holder.itemView.setOnClickListener {
            Log.d("TEST","ini test")
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvType: TextView
        var tvStatus: TextView
        var tvDate: TextView
        var tvTime: TextView

        init {
            tvType = itemView.findViewById(R.id.type)
            tvStatus = itemView.findViewById(R.id.status)
            tvDate = itemView.findViewById(R.id.date)
            tvTime = itemView.findViewById(R.id.time)
        }
    }
}