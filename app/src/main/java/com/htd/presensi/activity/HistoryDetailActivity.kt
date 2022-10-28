package com.htd.presensi.activity

import android.app.DownloadManager
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.htd.presensi.databinding.ActivityHistoryDetailBinding
import com.htd.presensi.models.Presence
import com.htd.presensi.rest.ApiClient
import com.htd.presensi.rest.ApiInterface
import com.htd.presensi.util.Loading
import com.htd.presensi.viewmodel.MainViewModel
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONException
import org.json.JSONObject
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

    val BASE_URL = ApiClient.BASE_URL+"storage/"

    lateinit var loading:Loading

    var mainViewModel: MainViewModel = MainViewModel()
    val REQUEST_FILE_LUAR_LOKASI = 4
    val FILE_NAME = "presence.jpg"

    lateinit var webView: WebView

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

        supportActionBar?.title = "Detail Riwayat Kehadiran"
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
            if (data.pic_url == null) {
                binding.image.visibility = View.GONE
            } else {
                Glide.with(this@HistoryDetailActivity).load(BASE_URL+data.pic_url).into(binding.image)
            }
            Log.d(packageName,data.toString())

            if(data.attachment_url != null) {
                binding.attachment.visibility = View.GONE
                binding.btnAttachment.text = "Download"
                binding.btnAttachment.visibility = View.VISIBLE
                binding.btnView.visibility = View.VISIBLE

                binding.btnView.setOnClickListener {
                    var intent = Intent(applicationContext,WebViewActivity::class.java)
                    intent.putExtra("data",BASE_URL+data.attachment_url)
                    startActivity(intent)
                }

                binding.btnAttachment.setOnClickListener {
                    downloadFile(BASE_URL+data.attachment_url)
                }
            }else if(data.status == "diajukan"){
                binding.attachment.visibility = View.GONE
                binding.btnAttachment.text = "Upload Lampiran"
                binding.btnAttachment.visibility = View.VISIBLE

                binding.btnAttachment.setOnClickListener {
                    val intent = Intent()
                        .setType("*/*")
                        .setAction(Intent.ACTION_GET_CONTENT)

                    startActivityForResult(Intent.createChooser(intent, "Select a file"), REQUEST_FILE_LUAR_LOKASI)
                }
            }

            if(data.type!!.contains("Cuti")){
                supportActionBar?.title = "Detail Riwayat Cuti"
            } else if(data.type == "tugas luar") {
                supportActionBar?.title = "Detail Riwayat Tugas Luar / Dalam"
            }

            if(data.type!!.contains("Cuti") || data.type == "tugas luar"){
                binding.llStatus.visibility = View.GONE
                binding.inLocation.text = "-"
                binding.txtTanggal.text = "Tanggal Pengajuan"
                binding.dateStart.text = data.started_at
                binding.dateEnd.text = data.finished_at
                binding.llStart.visibility = View.VISIBLE
                binding.llEnd.visibility = View.VISIBLE
                binding.llInLocation.visibility = View.GONE
                binding.llKeterlambatan.visibility = View.GONE
                binding.llPersentase.visibility = View.GONE
            }else{
                binding.status.text = data.worktimeItem?.capitalize()
                binding.inLocation.text = if(data.in_location!!) "Ya" else "Tidak"
                binding.timeLeft.text = data.time_left
                binding.persentase.text = data.persentase
            }
            binding.type.text = data.type?.capitalize() + " ("+data.status+")"
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

                Log.d("DATA",data.toString())

                val date = sdf.parse(data.get("created_at").asString)
                val startedAt = if(data.get("started_at") != null) sdf.parse(data.get("started_at").asString) else null
                val finishedAt = if(data.get("finished_at") != null) sdf.parse(data.get("finished_at").asString) else null

                var presence = Presence()
                presence.id = data.get("id").asString
                presence.attachment_url = data.get("attachment_url")?.asString
                presence.pic_url = data.get("pic_url")?.asString
                presence.lat = data.get("lat")?.asString
                presence.lng = data.get("lng")?.asString
                presence.type = data.get("type").asString
                presence.status = data.get("status").asString
                presence.in_location = if(data.get("in_location").asInt == 1) true else false
                presence.worktimeItem = if(data.get("worktime_item") != null) data.get("worktime_item").asJsonObject.get("name").asString else ""
                presence.date = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id")).format(date)
                presence.time = SimpleDateFormat("HH:mm").format(date)
                presence.started_at = if(startedAt != null) SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id")).format(startedAt) else null
                presence.finished_at = if(finishedAt != null) SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id")).format(finishedAt) else null

                if(data.get("time_left") != null){

                    var time_left_int = data.get("time_left").asInt
                    var time_left = ""

                    // terlalu cepat
                    if(time_left_int < 0)
                    {
                        binding.tvTimeLeft.text = "Sebelum Waktu"
                        time_left = "${time_left_int} Menit"
                    }else if(time_left_int > 0)
                    {
                        binding.tvTimeLeft.text = "Keterlambatan"
                        time_left = "${time_left_int} Menit"
                    }else{
                        time_left = "-"
                    }

                    presence.time_left = time_left
                    presence.persentase = data.get("presentase").asString
                }

                mainViewModel.historyDetail.postValue(presence)

            }

            override fun onFailure(call: Call<Any>, t: Throwable) {
                Log.d(packageName, t.toString())
            }
        })
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == REQUEST_FILE_LUAR_LOKASI && resultCode == RESULT_OK) {
            if(data !=null){
                var isLuar = contentResolver.openInputStream(data.data!!)

                var imageDataLuar = isLuar?.buffered()?.use { it.readBytes() }
                var requestFileLuar = RequestBody.create(MediaType.parse("multipart/form-data"), imageDataLuar)
                var attachment = MultipartBody.Part.createFormData("attachment",FILE_NAME, requestFileLuar)
                loading.show()
                mApiInterface.uploadAttachment(userLoggedIn.getString("token",null)!!,userLoggedIn.getString("employee_id",null)!!,intent.getStringExtra("employee_presence_id").toString(),attachment).enqueue(object : Callback<Any> {
                    override fun onResponse(
                        call: Call<Any>,
                        response: Response<Any>
                    ) {
                        if(response.code() == 200){
                            Log.d(packageName, response.body().toString())

                            var res = Gson().toJsonTree(response.body()).asJsonObject
                            var data = res.getAsJsonObject("data")
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

                            val date = sdf.parse(data.get("created_at").asString)
                            val startedAt = if(data.get("started_at") != null) sdf.parse(data.get("started_at").asString) else null
                            val finishedAt = if(data.get("finished_at") != null) sdf.parse(data.get("finished_at").asString) else null

                            var presence = Presence()
                            presence.id = data.get("id").asString
                            presence.attachment_url = data.get("attachment_url")?.asString
                            presence.pic_url = data.get("pic_url")?.asString
                            presence.lat = data.get("lat")?.asString
                            presence.lng = data.get("lng")?.asString
                            presence.type = data.get("type").asString
                            presence.status = data.get("status").asString
                            presence.in_location = if(data.get("in_location").asInt == 1) true else false
                            presence.worktimeItem = if(data.get("worktime_item") != null) data.get("worktime_item").asJsonObject.get("name").asString else ""
                            presence.date = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id")).format(date)
                            presence.time = SimpleDateFormat("HH:mm").format(date)
                            presence.started_at = if(startedAt != null) SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id")).format(startedAt) else null
                            presence.finished_at = if(finishedAt != null) SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id")).format(finishedAt) else null

                            if(data.get("time_left") != null){

                                var time_left_int = data.get("time_left").asInt
                                var time_left = ""

                                // terlalu cepat
                                if(presence.worktimeItem == "Pulang")
                                {
                                    binding.tvTimeLeft.text = "Sebelum Waktu"
                                    time_left = "${time_left_int} Menit"
                                }else if(presence.worktimeItem == "Masuk")
                                {
                                    binding.tvTimeLeft.text = "Keterlambatan"
                                    time_left = "${time_left_int} Menit"
                                }else{
                                    time_left = "-"
                                }

                                presence.time_left = time_left
                                presence.persentase = data.get("presentase").asString
                            }

                            mainViewModel.historyDetail.postValue(presence)

                            showAlert("Berhasil Upload")
                        }else{

                            showAlert("Upload Gagal! File Melebihi 1MB")
//
//                            var jsonObject: JSONObject? = null
//                            try {
//                                jsonObject = JSONObject(response.errorBody().string())
//                                val message: String = jsonObject.getString("message")
//                                showAlert(message)
////                        Toast.makeText(applicationContext,message,Toast.LENGTH_LONG).show()
//                            } catch (e: JSONException) {
//                                e.printStackTrace()
//                            }
                        }
                        Log.d(packageName, response.raw().toString())
                        loading.hide()
                    }

                    override fun onFailure(call: Call<Any>, t: Throwable) {
                        Log.d(packageName, t.toString())
                        showAlert("Ada Kesalahan Server")
//                Toast.makeText(applicationContext,"Ada Kesalahan Server",Toast.LENGTH_LONG).show()
                        loading.hide()
                    }
                })
            }

        }
    }

    fun showAlert(message:String){
        var alert = AlertDialog.Builder(this)
        alert.setMessage(message)
        alert.setPositiveButton("Ok"){dialog,_->
            dialog.dismiss()
        }
        alert.show()
    }

}