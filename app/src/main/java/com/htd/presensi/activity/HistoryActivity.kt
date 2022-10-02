package com.htd.presensi.activity

import android.R
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.htd.presensi.adapter.HistoryAdapter
import com.htd.presensi.databinding.ActivityHistoryBinding
import com.htd.presensi.models.Presence
import com.htd.presensi.models.Report
import com.htd.presensi.rest.ApiClient
import com.htd.presensi.rest.ApiInterface
import com.htd.presensi.util.CustomDatePickerDialog
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
import java.text.SimpleDateFormat
import java.util.*


class HistoryActivity : AppCompatActivity(), View.OnClickListener,
    AdapterView.OnItemSelectedListener {

    lateinit var binding: ActivityHistoryBinding
    lateinit var userLoggedIn: SharedPreferences
    lateinit var mApiInterface: ApiInterface
    lateinit var jenisAbsensiAdapter: ArrayAdapter<String>
    lateinit var statusAdapter: ArrayAdapter<String>
    lateinit var lampiranTugasLuarText : TextView
    lateinit var lampiranCutiText : TextView
    lateinit var cutiAdapter: ArrayAdapter<String>

    var selectedCuti = ""
    var selectedTugas = ""
    var attachmentUri : Uri? = null
    val FILE_NAME = "presence.jpg"
    val REQUEST_FILE_TUGAS_LUAR = 5
    val REQUEST_FILE_CUTI = 6

    lateinit var loading:Loading

    lateinit var historyAdapter: HistoryAdapter
    var mainViewModel: MainViewModel = MainViewModel()

    var cuti:ArrayList<String> = ArrayList()

    var jenisAbsensi = arrayOf("Semua","Hadir","Izin","Sakit")
    var status = arrayOf("Semua","Diterima","Ditolak","Diajukan")

    var selectedJenisAbsensi = "Semua"
    var selectedStatus = "Semua"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        observe()
        api()
        listener()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        loading.dismiss()
        return true
    }

    fun init(){
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        historyAdapter = HistoryAdapter(this)
        mApiInterface = ApiClient.client!!.create(ApiInterface::class.java)

        if(intent.hasExtra("type")){
            if(intent.extras!!.getString("type") == "cuti"){
                supportActionBar?.title = "Riwayat Cuti"
            }else{
                supportActionBar?.title = "Riwayat Tugas Luar / Dalam"
            }
        }else{
            supportActionBar?.title = "Riwayat Absensi"
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        userLoggedIn = getSharedPreferences("login_data", MODE_PRIVATE)

        jenisAbsensiAdapter = ArrayAdapter(this, R.layout.simple_spinner_item, jenisAbsensi)
        jenisAbsensiAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.filter.jenisAbsensi.adapter = jenisAbsensiAdapter

        statusAdapter = ArrayAdapter(this, R.layout.simple_spinner_item, status)
        statusAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.filter.status.adapter = statusAdapter

        binding.filter.fromDate.text = CustomDatePickerDialog.year.toString()+"-"+(CustomDatePickerDialog.month+1).toString()+"-01"
        binding.filter.toDate.text = CustomDatePickerDialog.year.toString()+"-"+(CustomDatePickerDialog.month+1).toString()+"-"+CustomDatePickerDialog.day.toString()

        binding.rvPresences.layoutManager = LinearLayoutManager(this)
        binding.rvPresences.adapter = historyAdapter

        loading = Loading(this)

    }

    fun observe(){
        mainViewModel.histories.observe(this) { data ->
            historyAdapter.data = data
            historyAdapter.notifyDataSetChanged()
        }

        mainViewModel.reports.observe(this) { data ->
            binding.hadir.text = data.hadir
            binding.alfa.text = data.alfa
            binding.cuti.text = data.cuti
            binding.hariKerja.text = data.hari_kerja
            binding.waktuTelat.text = data.waktu_telat
            binding.persentase.text = data.persentase
        }

        mainViewModel.paidLeaves.observe(this){data->
            cuti = data
            cutiAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cuti)
            cutiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    fun api(){
        getData(binding.filter.fromDate.text.toString(),binding.filter.toDate.text.toString())
    }

    fun listener(){
        binding.filter.fromDate.setOnClickListener(this)
        binding.filter.toDate.setOnClickListener(this)
        binding.filter.filterBtn.setOnClickListener(this)
        binding.filter.jenisAbsensi.setOnItemSelectedListener(this)
        binding.filter.status.setOnItemSelectedListener(this)
        binding.addFab.setOnClickListener(this)
    }

    fun openPengajuanDialog(){

        var intentType = intent.extras!!.getString("type")
        if (intentType.equals("tugas luar"))
        {
            openTugasLuar()
        }

        if(intentType.equals("cuti"))
        {
            openCuti()
        }

    }

    fun openCuti(){

        val dialog = Dialog(this)
        dialog.setCancelable(false)
        dialog.setContentView(com.htd.presensi.R.layout.cuti)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        var btnWaktuMulai = dialog.findViewById(com.htd.presensi.R.id.btn_waktu_mulai) as Button
        var btnWaktuSelesai = dialog.findViewById(com.htd.presensi.R.id.btn_waktu_selesai) as Button
        var btnLampiran = dialog.findViewById(com.htd.presensi.R.id.btn_lampiran) as Button
        lampiranCutiText = dialog.findViewById(com.htd.presensi.R.id.tv_lampiran) as TextView
        lampiranCutiText.text = "Pilih File Lebih Dahulu"

        var tvWaktuMulai = dialog.findViewById(com.htd.presensi.R.id.tv_waktu_mulai) as TextView
        var tvWaktuSelesai = dialog.findViewById(com.htd.presensi.R.id.tv_waktu_selesai) as TextView

        val yesBtn = dialog.findViewById(com.htd.presensi.R.id.submit) as Button
        val noBtn = dialog.findViewById(com.htd.presensi.R.id.cancel) as Button

        val jenisPengajuan = dialog.findViewById(com.htd.presensi.R.id.jenis_pengajuan) as Spinner
        jenisPengajuan.adapter = cutiAdapter

        jenisPengajuan.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectedCuti = cuti.get(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })

        btnLampiran.setOnClickListener {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, "Select a file"), REQUEST_FILE_TUGAS_LUAR)
        }

        btnWaktuMulai.setOnClickListener{
            tvWaktuMulai.text = CustomDatePickerDialog.year.toString()+"-"+(CustomDatePickerDialog.month+1).toString()+"-01"
            CustomDatePickerDialog.show(this, dialog.findViewById(com.htd.presensi.R.id.tv_waktu_mulai))
        }

        btnWaktuSelesai.setOnClickListener{
            tvWaktuSelesai.text = CustomDatePickerDialog.year.toString()+"-"+(CustomDatePickerDialog.month+1).toString()+"-01"
            CustomDatePickerDialog.show(this, dialog.findViewById(com.htd.presensi.R.id.tv_waktu_selesai))
        }

        btnLampiran.setOnClickListener {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, "Select a file"), REQUEST_FILE_CUTI)
        }

        yesBtn.setOnClickListener {
            if(selectedCuti != null && attachmentUri != null && tvWaktuMulai.text.toString() != null && tvWaktuSelesai.text.toString() != null){
                presences(selectedCuti,attachmentUri!!,tvWaktuMulai.text.toString(),tvWaktuSelesai.text.toString())
                dialog.dismiss()
            }else{
                showAlert("Pilih Cuti/Tanggal/File Lebih Dulu")
            }
        }

        noBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()

    }

    fun openTugasLuar()
    {
        val dialog = Dialog(this)
        dialog.setCancelable(false)
        dialog.setContentView(com.htd.presensi.R.layout.tugas_luar)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        var btnWaktuMulai = dialog.findViewById(com.htd.presensi.R.id.btn_waktu_mulai) as Button
        var btnWaktuSelesai = dialog.findViewById(com.htd.presensi.R.id.btn_waktu_selesai) as Button
        var btnLampiran = dialog.findViewById(com.htd.presensi.R.id.btn_lampiran) as Button
        lampiranTugasLuarText = dialog.findViewById(com.htd.presensi.R.id.tv_lampiran) as TextView
        lampiranTugasLuarText.text = "Pilih File Lebih Dahulu"

        val arraySpinner = arrayOf(
            "tugas luar", "tugas dalam"
        )

        val adapter = ArrayAdapter(
            this,
            R.layout.simple_spinner_item, arraySpinner
        )
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)

        val jenisPengajuan = dialog.findViewById(com.htd.presensi.R.id.jenis_pengajuan) as Spinner
        jenisPengajuan.adapter = adapter

        jenisPengajuan.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectedTugas = arraySpinner[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })

        var tvWaktuMulai = dialog.findViewById(com.htd.presensi.R.id.tv_waktu_mulai) as TextView
        var tvWaktuSelesai = dialog.findViewById(com.htd.presensi.R.id.tv_waktu_selesai) as TextView

        val yesBtn = dialog.findViewById(com.htd.presensi.R.id.submit) as Button
        val noBtn = dialog.findViewById(com.htd.presensi.R.id.cancel) as Button

        btnWaktuMulai.setOnClickListener{
            tvWaktuMulai.text = CustomDatePickerDialog.year.toString()+"-"+(CustomDatePickerDialog.month+1).toString()+"-"+CustomDatePickerDialog.day.toString()
            CustomDatePickerDialog.show(this, dialog.findViewById(com.htd.presensi.R.id.tv_waktu_mulai))
        }

        btnWaktuSelesai.setOnClickListener{
            tvWaktuSelesai.text = CustomDatePickerDialog.year.toString()+"-"+(CustomDatePickerDialog.month+1).toString()+"-"+CustomDatePickerDialog.day.toString()
            CustomDatePickerDialog.show(this, dialog.findViewById(com.htd.presensi.R.id.tv_waktu_selesai))
        }

        btnLampiran.setOnClickListener {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, "Select a file"), REQUEST_FILE_TUGAS_LUAR)
        }

        yesBtn.setOnClickListener {
            if(attachmentUri != null && tvWaktuMulai.text.toString() != null && tvWaktuSelesai.text.toString() != null){
                presences(selectedTugas,attachmentUri!!,tvWaktuMulai.text.toString(),tvWaktuSelesai.text.toString())
                dialog.dismiss()
            }else{
                showAlert("Pilih Tanggal/File Lebih Dulu")
            }
        }

        noBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    fun getData(start: String, end: String) {
        loading.show()

        if(intent.hasExtra("type")){
            binding.detail.visibility = View.GONE

            if(intent.extras!!.getString("type") == "cuti"){
                getPaidLeaves()

                mApiInterface.getHistoryCuti(userLoggedIn.getString("token",null)!!,userLoggedIn.getString("employee_id",null)!!,start,end).enqueue(object :
                    Callback<Any> {
                    override fun onResponse(
                        call: Call<Any>,
                        response: Response<Any>
                    ) {
                        if(response.body() != null){
                            var res = Gson().toJsonTree(response.body()).asJsonObject
                            var data = res.getAsJsonObject("data")

                            Log.d("cuti",data.toString())

                            val presences = data.getAsJsonArray("presences")
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

                            var arrPresences = ArrayList<Presence>()

                            for(p in presences){
                                val obj = p.asJsonObject
                                val started_at = sdf.parse(obj.get("started_at").asString)
                                val finished_at = sdf.parse(obj.get("finished_at").asString)
                                val created_at = sdf.parse(obj.get("created_at").asString)

                                var presence = Presence()
                                presence.id = obj.get("id").asString
                                presence.attachment_url = obj.get("attachment_url")?.asString
                                presence.type = obj.get("type").asString
                                presence.in_location = obj.get("in_location").asBoolean
                                presence.status = obj.get("status").asString
                                presence.started_at = SimpleDateFormat("dd MMMM yyyy", Locale("id")).format(started_at)
                                presence.finished_at = SimpleDateFormat("dd MMMM yyyy", Locale("id")).format(finished_at)
                                presence.date = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id")).format(created_at)
                                presence.time = SimpleDateFormat("HH:mm").format(created_at)

                                arrPresences.add(presence)
                            }

                            mainViewModel.histories.postValue(arrPresences)
                        }

                        loading.hide()

                    }

                    override fun onFailure(call: Call<Any>, t: Throwable) {
                        Log.d(packageName, t.toString())
                        loading.hide()
                    }
                })
            }else {

                mApiInterface.getPresences(userLoggedIn.getString("token",null)!!,userLoggedIn.getString("employee_id",null)!!,"tugas luar",if(selectedStatus == "Semua") "" else selectedStatus,start,end).enqueue(object :
                    Callback<Any> {
                    override fun onResponse(
                        call: Call<Any>,
                        response: Response<Any>
                    ) {

                        if(response.body() != null){
                            var res = Gson().toJsonTree(response.body()).asJsonObject
                            var data = res.getAsJsonObject("data")

                            Log.d("tugas luar",data.toString())

                            val presences = data.getAsJsonArray("presences")

                            var arrPresences = ArrayList<Presence>()
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

                            for(p in presences){
                                val obj = p.asJsonObject
                                val started_at = sdf.parse(obj.get("started_at").asString)
                                val finished_at = sdf.parse(obj.get("finished_at").asString)
                                val created_at = sdf.parse(obj.get("created_at").asString)

                                var presence = Presence()
                                presence.id = obj.get("id").asString
                                presence.attachment_url = obj.get("attachment_url")?.asString
                                presence.type = obj.get("type").asString
                                presence.in_location = obj.get("in_location").asBoolean
                                presence.status = obj.get("status").asString
                                presence.started_at = SimpleDateFormat("dd MMMM yyyy", Locale("id")).format(started_at)
                                presence.finished_at = SimpleDateFormat("dd MMMM yyyy", Locale("id")).format(finished_at)
                                presence.date = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id")).format(created_at)
                                presence.time = SimpleDateFormat("HH:mm").format(created_at)

                                arrPresences.add(presence)
                            }

                            mainViewModel.histories.postValue(arrPresences)
                        }
                        loading.hide()

                    }

                    override fun onFailure(call: Call<Any>, t: Throwable) {
                        Log.d(packageName, t.toString())
                        loading.hide()
                    }
                })
            }
        }else{
            binding.addFab.visibility = View.GONE

            mApiInterface.getPresences(userLoggedIn.getString("token",null)!!,userLoggedIn.getString("employee_id",null)!!,if(selectedJenisAbsensi == "Semua") "" else selectedJenisAbsensi,if(selectedStatus == "Semua") "" else selectedStatus,start,end).enqueue(object :
                Callback<Any> {
                override fun onResponse(
                    call: Call<Any>,
                    response: Response<Any>
                ) {
                    if(response.body() != null){
                        var res = Gson().toJsonTree(response.body()).asJsonObject
                        var data = res.getAsJsonObject("data")

                        Log.d("histories",data.toString())


                        val presences = data.getAsJsonArray("presences")

                        var arrPresences = ArrayList<Presence>()
                        val sdf = SimpleDateFormat("yyyy-MM-dd")

                        for(p in presences){
                            val obj = p.asJsonObject
                            val date = sdf.parse(obj.get("date").asString)

                            var presence = Presence()
                            presence.id = obj.get("id").asString
                            presence.attachment_url = obj.get("attachment_url")?.asString
                            presence.pic_url = obj.get("pic_url")?.asString
                            presence.lat = obj.get("lat")?.asString
                            presence.lng = obj.get("lng")?.asString
                            presence.type = obj.get("type").asString
                            presence.in_location = obj.get("in_location").asBoolean
                            presence.status = obj.get("status").asString
                            presence.worktimeItem = if(obj.get("worktime_item") != null) obj.get("worktime_item").asJsonObject.get("name").asString else ""
                            presence.date = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id")).format(date)
                            presence.time = obj.get("time")?.asString
                            presence.persentase = obj.get("presentase")?.asString

                            if(obj.get("time_left") != null){

                                var time_left_int = obj.get("time_left").asInt
                                var time_left = ""

                                // terlalu cepat
                                if(presence.worktimeItem == "Masuk")
                                {
                                    time_left = "Keterlambatan "+time_left_int+" Menit ("+presence.persentase+"%)"
                                }else if(presence.worktimeItem == "Pulang")
                                {
                                    time_left = "Sebelum Waktu "+time_left_int+" Menit ("+presence.persentase+"%)"
                                }else{
                                    time_left = "Tepat Waktu"
                                }

                                presence.time_left = time_left
                            }

                            arrPresences.add(presence)
                        }

                        mainViewModel.histories.postValue(arrPresences)
                    }

                    loading.hide()

                }

                override fun onFailure(call: Call<Any>, t: Throwable) {
                    Log.d(packageName, t.toString())
                    loading.hide()
                }
            })

            mApiInterface.reports(userLoggedIn.getString("token",null)!!,userLoggedIn.getString("workunit_id",null)!!,start,end,userLoggedIn.getString("name",null)!!).enqueue(object : Callback<Any> {
                override fun onResponse(
                    call: Call<Any>,
                    response: Response<Any>
                ) {
                    if(response.body() != null){
                        var res = Gson().toJsonTree(response.body()).asJsonObject
                        var dt = res.getAsJsonObject("data")
                        var data = dt.getAsJsonArray("data")
                        var report = Report()
                        if(data.size() > 0){
                            report.hadir = data.get(0).asJsonObject.get("hadir").asInt.toString()
                            report.alfa = data.get(0).asJsonObject.get("alfa").asInt.toString()
                            report.cuti = data.get(0).asJsonObject.get("cuti").asInt.toString()
                            report.hari_kerja = data.get(0).asJsonObject.get("hari_kerja").asInt.toString()
                            report.waktu_telat = data.get(0).asJsonObject.get("time_left").asInt.toString()
                            report.persentase = data.get(0).asJsonObject.get("presentase").asString
                        }else{
                            report.hadir = "0"
                            report.alfa = "0"
                            report.cuti = "0"
                            report.hari_kerja = "0"
                            report.waktu_telat = "0"
                            report.persentase = "0%"
                        }
                        mainViewModel.reports.postValue(report)
                        Log.d("reports",data.toString())
                    }
                }

                override fun onFailure(call: Call<Any>, t: Throwable) {
                    Log.d(packageName, t.toString())
                }
            })

        }
    }

    fun getPaidLeaves(){
        mApiInterface.getPaidLeaves(userLoggedIn.getString("token",null)!!).enqueue(object : Callback<Any>{
            override fun onResponse(call: Call<Any>?, response: Response<Any>) {
                if(response.code() == 200){
                    Log.d(packageName, response.body().toString())
                    var res = Gson().toJsonTree(response.body()).asJsonObject
                    var data = res.get("data").asJsonObject
                    var dataArr = data.get("data").asJsonArray
                    var arrs: ArrayList<String> = ArrayList()
                    for (i in 0 until dataArr.size()) {
                        var dt = dataArr.get(i).asJsonObject
                        arrs.add(dt.get("name").asString)
                    }
                    mainViewModel.paidLeaves.postValue(arrs)
                }else{
                    var jsonObject: JSONObject? = null
                    try {
                        jsonObject = JSONObject(response.errorBody().string())
                        val message: String = jsonObject.getString("message")
                        Toast.makeText(applicationContext,message,Toast.LENGTH_SHORT).show()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
                Log.d(packageName, response.body().toString())
            }

            override fun onFailure(call: Call<Any>?, t: Throwable?) {
                Log.d(packageName, t.toString())
            }

        })
    }

    fun presences(type:String, uriFile: Uri, waktuMulai : String? = "", waktuSelesai: String? = ""){
        loading.show()

        var pic_url = MultipartBody.Part.createFormData("pic","")

        var attachment: MultipartBody.Part? = null
        if(attachmentUri != null){
            var isLuar = contentResolver.openInputStream(attachmentUri!!)

            var imageDataLuar = isLuar?.buffered()?.use { it.readBytes() }
            var requestFileLuar = RequestBody.create(MediaType.parse("multipart/form-data"), imageDataLuar)
            attachment = MultipartBody.Part.createFormData("attachment",FILE_NAME, requestFileLuar)
        }else{
            attachment = MultipartBody.Part.createFormData("attachment","")
        }

        val typeBody: RequestBody = RequestBody.create(MediaType.parse("multipart/form-data"), type)
        val lngBody = RequestBody.create(MediaType.parse("multipart/form-data"), "")
        val latBody = RequestBody.create(MediaType.parse("multipart/form-data"), "")
        val inLocationBody = RequestBody.create(MediaType.parse("multipart/form-data"), "")
        val worktimeItemId = RequestBody.create(MediaType.parse("multipart/form-data"), "")
        val startedAt = RequestBody.create(MediaType.parse("multipart/form-data"),waktuMulai)
        val finishedAt = RequestBody.create(MediaType.parse("multipart/form-data"),waktuSelesai)

        var employee_id = userLoggedIn.getString("employee_id",null)

        mApiInterface.presences(
            userLoggedIn.getString("token",null)!!,
            employee_id!!,
            typeBody,
            attachment,
            latBody,
            lngBody,
            inLocationBody,
            pic_url,
            worktimeItemId,
            startedAt,
            finishedAt
        ).enqueue(object : Callback<Any> {
            override fun onResponse(
                call: Call<Any>,
                response: Response<Any>
            ) {
                if(response.code() == 200){
                    Log.d(packageName, response.body().toString())
//                    Toast.makeText(applicationContext,"Berhasil",Toast.LENGTH_LONG).show()
                    showAlert("Pengajuan berhasil!")
                    attachmentUri = null
                    api()
                }else{
                    var jsonObject: JSONObject? = null
                    try {
                        jsonObject = JSONObject(response.errorBody().string())
                        val message: String = jsonObject.getString("message")
                        showAlert(message)
//                        Toast.makeText(applicationContext,message,Toast.LENGTH_LONG).show()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
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

    fun showAlert(message:String){
        var alert = AlertDialog.Builder(this)
        alert.setMessage(message)
        alert.setPositiveButton("Ok"){dialog,_->
            dialog.dismiss()
        }
        alert.show()
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            binding.filter.fromDate.id -> CustomDatePickerDialog.show(this, binding.filter.fromDate)
            binding.filter.toDate.id -> CustomDatePickerDialog.show(this, binding.filter.toDate)
            binding.filter.filterBtn.id -> getData(
                binding.filter.fromDate.text.toString(),
                binding.filter.toDate.text.toString()
            )
            binding.addFab.id -> openPengajuanDialog()
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, view: View?, idx: Int, p3: Long) {
        when(p0?.id){
            binding.filter.jenisAbsensi.id -> {
                selectedJenisAbsensi = jenisAbsensi[idx]
            }
            binding.filter.status.id -> selectedStatus = status[idx]
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == REQUEST_FILE_TUGAS_LUAR && resultCode == RESULT_OK) {
            if(data !=null){
                attachmentUri = data.data!!
                lampiranTugasLuarText.text  = getFileName(attachmentUri!!)
            }
        }

        if(requestCode == REQUEST_FILE_CUTI && resultCode == RESULT_OK) {
            if(data !=null){
                attachmentUri = data.data!!
                lampiranCutiText.text  = getFileName(attachmentUri!!)
            }
        }
    }

    @SuppressLint("Range")
    fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor!!.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

}