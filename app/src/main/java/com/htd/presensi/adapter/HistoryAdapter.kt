package com.htd.presensi.adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
        if(dt.type!!.contains("Cuti") || dt.type == "tugas luar"){
            holder.tvStatus.text = dt.type?.capitalize()
            holder.tvDate.text = dt.date
            holder.tvType.text = dt.started_at
            holder.tvInLocation.text = dt.finished_at
            holder.tvTimeLeft.text = dt.status?.capitalize()
            holder.tvTime.text = ""
        }else{
            holder.tvStatus.text = dt.worktimeItem?.capitalize()
            holder.tvInLocation.text = if(dt.time == "false") "" else if(dt.in_location!!) "di lokasi" else "di luar lokasi"
            holder.tvType.text = dt.type?.capitalize() + if(dt.status == "false") "" else " ("+dt.status+")"
            holder.tvDate.text = dt.date
            holder.tvTimeLeft.text = dt.time_left
            holder.tvTime.text = if(dt.time == "false") "" else dt.time
        }

        holder.itemView.setOnClickListener {
            if(dt.time != "false"){
                var intent = Intent(context, HistoryDetailActivity::class.java)
                intent.putExtra("employee_presence_id",dt.id)
                context.startActivity(intent)
            }else{
                Toast.makeText(context,"Tidak ada data!",Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvType: TextView
        var tvStatus: TextView
        var tvDate: TextView
        var tvInLocation: TextView
        var tvTime: TextView
        var tvTimeLeft: TextView

        init {
            tvType = itemView.findViewById(R.id.type)
            tvInLocation = itemView.findViewById(R.id.in_location)
            tvStatus = itemView.findViewById(R.id.status)
            tvDate = itemView.findViewById(R.id.date)
            tvTime = itemView.findViewById(R.id.time)
            tvTimeLeft = itemView.findViewById(R.id.time_left)
        }
    }
}