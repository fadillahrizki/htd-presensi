package com.htd.presensi.adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.htd.presensi.R
import com.htd.presensi.activity.HistoryDetailActivity
import com.htd.presensi.activity.ProfileActivity
import com.htd.presensi.models.Presence

class HistoryAdapter(var context: Context) : RecyclerView.Adapter<HistoryAdapter.MyViewHolder>() {
    var data: ArrayList<Presence> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder = MyViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.presence, parent, false))

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val dt = data[position]
        holder.tvStatus.text = dt.worktimeItem?.capitalize()
        holder.tvType.text = dt.type?.capitalize() + " ("+dt.status+")"
        holder.tvDate.text = dt.date
        holder.tvTime.text = dt.time

        holder.itemView.setOnClickListener {
            var intent = Intent(context, HistoryDetailActivity::class.java)
            intent.putExtra("employee_presence_id",dt.id)
            context.startActivity(intent)
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