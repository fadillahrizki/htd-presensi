package com.htd.presensi.activity

import android.app.DownloadManager
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.htd.presensi.databinding.ActivityHistoryDetailBinding
import com.htd.presensi.models.Presence
import com.htd.presensi.rest.ApiClient
import com.htd.presensi.rest.ApiInterface
import com.htd.presensi.util.Loading
import com.htd.presensi.viewmodel.MainViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URL
import java.nio.channels.Channels
import java.text.SimpleDateFormat
import java.util.*


class HistoryDetailActivity : AppCompatActivity() {
    lateinit var binding: ActivityHistoryDetailBinding
    lateinit var userLoggedIn: SharedPreferences
    lateinit var mApiInterface: ApiInterface

    val BASE_URL = "https://api-presence.z-techno.com/storage/"

    lateinit var loading:Loading

    var mainViewModel: MainViewModel = MainViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        init()
        observe()
        api()
        listener()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        loading.dismiss()
        return true
    }

    fun init(){
        binding = ActivityHistoryDetailBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        mApiInterface = ApiClient.client!!.create(ApiInterface::class.java)

        supportActionBar?.title = "Detail Riwayat"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        userLoggedIn = getSharedPreferences("login_data", MODE_PRIVATE)
        loading = Loading(this)
    }

    fun downloadFile(url: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Lampiran_"+Calendar.getInstance().timeInMillis)
            .setDescription("Downloading...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)

        var dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)

        Toast.makeText(applicationContext,"Downloading...",Toast.LENGTH_SHORT).show()
    }

    fun observe(){
        mainViewModel.historyDetail.observe(this) { data ->
            Glide.with(this@HistoryDetailActivity).load(BASE_URL+data.pic_url).into(binding.image)

            if(data.attachment_url != null){
                binding.attachment.visibility = View.GONE
                binding.btnAttachment.visibility = View.VISIBLE

                binding.btnAttachment.setOnClickListener {
                    downloadFile(BASE_URL+data.attachment_url)
                }
            }


            binding.status.text = data.status?.capitalize()
            binding.type.text = data.type?.capitalize()
            binding.date.text = data.date+" ("+data.time+")"
            loading.hide()

            Log.d(packageName,BASE_URL+data.pic_url)
        }
    }

    fun api(){
        var id = intent.getStringExtra("employee_presence_id").toString()
        getData(id)
    }

    fun listener(){
    }


    fun getData(id: String) {
        loading.show()
        mApiInterface.getDetailPresence(userLoggedIn.getString("token",null)!!,userLoggedIn.getString("employee_id",null)!!,id).enqueue(object :
            Callback<Any> {
            override fun onResponse(
                call: Call<Any>,
                response: Response<Any>
            ) {
                var res = Gson().toJsonTree(response.body()).asJsonObject
                var data = res.getAsJsonObject("data")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

                val date = sdf.parse(data.get("created_at").asString)

                Log.d(packageName,data.toString())

                var presence = Presence()
                presence.id = data.get("id").asString
                presence.attachment_url = data.get("attachment_url")?.asString
                presence.pic_url = data.get("pic_url")?.asString
                presence.lat = data.get("lat")?.asString
                presence.lng = data.get("lng")?.asString
                presence.type = data.get("type").asString
                presence.status = data.get("status").asString
                presence.date = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id")).format(date)
                presence.time = SimpleDateFormat("HH:mm").format(date)

                mainViewModel.historyDetail.postValue(presence)

            }

            override fun onFailure(call: Call<Any>, t: Throwable) {
                Log.d(packageName, t.toString())
            }
        })
    }



}