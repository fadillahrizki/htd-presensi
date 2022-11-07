 package com.htd.presensi.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.htd.presensi.BuildConfig
import com.htd.presensi.R
import com.htd.presensi.databinding.ActivityMainBinding
import com.htd.presensi.models.WorktimeItem
import com.htd.presensi.rest.ApiClient
import com.htd.presensi.rest.ApiInterface
import com.htd.presensi.services.GpsService
import com.htd.presensi.util.CustomDatePickerDialog
import com.htd.presensi.util.Loading
import com.htd.presensi.util.LocationDistance
import com.htd.presensi.viewmodel.MainViewModel
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


 class MainActivity : AppCompatActivity(), View.OnClickListener,LocationListener {

    lateinit var binding: ActivityMainBinding
    lateinit var mApiInterface: ApiInterface
    lateinit var userLoggedIn: SharedPreferences
    lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var photoFile: File
    lateinit var photoUri: Uri
    lateinit var loading: Loading
    lateinit var places : JSONArray
    lateinit var alertDialogBuilder : AlertDialog.Builder
    lateinit var luarLokasiText : TextView
    lateinit var lampiranTugasLuarText : TextView
    lateinit var lampiranCutiText : TextView
    lateinit var cutiAdapter: ArrayAdapter<String>

    var absenTemanId = ""
    var selectedWorktimeId = ""
    var selectedCuti = ""

    var cuti:ArrayList<String> = ArrayList()

    val REQUEST_IMAGE_CAPTURE = 1
    val REQUEST_FILE_IZIN = 2
    val REQUEST_FILE_SAKIT = 3
    val REQUEST_FILE_LUAR_LOKASI = 4
    val REQUEST_FILE_TUGAS_LUAR = 5
    val REQUEST_FILE_CUTI = 6
    val PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 100
    val FILE_NAME = "presence.jpg"
    var currentLocation: Location? = null
    var inLocation = true
    var radius: Double? = 0.0
    var attachmentUri : Uri? = null

    var mainViewModel: MainViewModel = MainViewModel()

    var counts = emptyArray<Int>()
    var time = 0
    var count = "00:00:00"

    var locationReq: LocationRequest? = null

    private var broadcastReceiver: BroadcastReceiver? = null

    override fun onResume() {
        super.onResume()
        if (broadcastReceiver == null) {
            broadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent) {
                   Log.d("GPS:", intent.extras!!["coordinates"] as String)
                }
            }
        }
        registerReceiver(broadcastReceiver, IntentFilter("location_update"))
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationRequest()
        init()
        observe()
        api()
        listener()
    }

    fun init(){
//        val i = Intent(applicationContext, GPS_Service::class.java)
//        startService(i)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        mApiInterface = ApiClient.client!!.create(ApiInterface::class.java)
        userLoggedIn = getSharedPreferences("login_data", MODE_PRIVATE)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        loading = Loading(this)
        places = JSONTokener(userLoggedIn.getString("places",null)).nextValue() as JSONArray
        alertDialogBuilder = AlertDialog.Builder(this)
        radius = userLoggedIn.getString("radius",null)?.toDouble()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_ACCESS_FINE_LOCATION)
        }else {
            fusedLocationClient.requestLocationUpdates(locationReq, getPendingIntent())
        }

        binding.name.text = "Hai, "+userLoggedIn.getString("name",null)

//        if(radius == null){
//            binding.absen.visibility = View.GONE
//        }
    }

    fun observe(){
        mainViewModel.activeWorktime.observe(this){data->
            if(data != null){
                selectedWorktimeId = data.id!!
                if(absenTemanId != ""){
                    getLocation()
                }else{
                    checkIfExists()
                }
            }else{
                showAlert("Maaf! Saat ini tidak ada jadwal presensi")
            }
        }

        mainViewModel.times.observe(this){data->
            count = data
        }

        mainViewModel.paidLeaves.observe(this){data->
            cuti = data
            cutiAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cuti)
            cutiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                counts = count.split(':').map{it.toInt()}.toTypedArray()
                time = counts[2]
                time += counts[1] * 60
                time += (counts[0] * 60) * 60

                if (counts[1] >= 59 && counts[0] > 0) {
                    counts[0] = counts[0] + 1
                    counts[1] = 0
                }

                if (counts[2] >= 59 && counts[1] > 0) {
                    counts[1] = counts[1] + 1
                    counts[2] = 0
                }

                if (counts[0] >= 59) {
                    counts[0] = 0
                    counts[1] = 0
                    counts[2] = 0
                }

                counts[2]++
                time++

                count = "${counts[0]}:${counts[1]}:${counts[2]}"

                binding.times.text = count
                mainHandler.postDelayed(this, 1000)
            }
        })
    }

    fun api(){
        getTimes()
        getPaidLeaves()
    }

    fun getWorktime(){
        mApiInterface.profile(userLoggedIn.getString("token",null)!!,userLoggedIn.getString("employee_id",null)!!).enqueue(object : Callback<Any>{
            override fun onResponse(call: Call<Any>?, response: Response<Any>) {
                if(response.code() == 200){
                    Log.d(packageName, response.body().toString())
                    var res = Gson().toJsonTree(response.body()).asJsonObject
                    var data = res.getAsJsonObject("data")

                    val activeWorktime = data.getAsJsonObject("active_worktime")

                    if(data.get("is_holiday").asBoolean){
                        showAlert("Maaf! Sekarang Sedang Libur.")
                    }else{

                        if(activeWorktime != null){
                            var worktimeItem = WorktimeItem()
                            worktimeItem.id = activeWorktime.get("id").asString
                            worktimeItem.worktime_id = activeWorktime.get("worktime_id")?.asString
                            worktimeItem.name = activeWorktime.get("name")?.asString
                            worktimeItem.start_time = activeWorktime.get("start_time")?.asString
                            worktimeItem.end_time = activeWorktime.get("end_time")?.asString
                            worktimeItem.on_time_start = activeWorktime.get("on_time_start").asString
                            worktimeItem.on_time_end = activeWorktime.get("on_time_end").asString

                            mainViewModel.activeWorktime.postValue(worktimeItem)

                        }else{
                            showAlert("Maaf! Saat ini tidak ada jadwal presensi")
                        }
                    }
                }else{
                    var jsonObject: JSONObject? = null
                    try {
                        jsonObject = JSONObject(response.errorBody().string())
                        val message: String = jsonObject.getString("message")
                        showAlert("Maaf! Saat ini tidak ada jadwal presensi")
//                        showAlert(message)
//                        Toast.makeText(applicationContext,message,Toast.LENGTH_LONG).show()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
                Log.d(packageName, response.raw().toString())
                loading.hide()
            }

            override fun onFailure(call: Call<Any>?, t: Throwable?) {
                Log.d(packageName, t.toString())
                showAlert("Ada Kesalahan Server")
//                Toast.makeText(applicationContext,"Ada Kesalahan Server",Toast.LENGTH_LONG).show()
                loading.hide()
            }

        })
    }

    fun getTimes(){
        mApiInterface.getTimes().enqueue(object : Callback<Any>{
            override fun onResponse(call: Call<Any>?, response: Response<Any>) {
                if(response.code() == 200){
                    Log.d(packageName, response.body().toString())
                    var res = Gson().toJsonTree(response.body()).asJsonObject
                    var data = res.get("data").asString
                    mainViewModel.times.postValue(data)
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
            }

            override fun onFailure(call: Call<Any>?, t: Throwable?) {
                Log.d(packageName, t.toString())
            }

        })
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

    fun listener(){
        binding.absen.setOnClickListener(this)
        binding.profil.setOnClickListener(this)
        binding.history.setOnClickListener(this)
        binding.historyCuti.setOnClickListener(this)
        binding.historyTugasLuar.setOnClickListener(this)
        binding.izinKerja.setOnClickListener(this)
        binding.absenTeman.setOnClickListener(this)
        binding.cuti.setOnClickListener(this)
        binding.tugasLuar.setOnClickListener(this)
        binding.sakit.setOnClickListener(this)
        binding.logout.setOnClickListener(this)
    }

    override fun onLocationChanged(location: Location) {
        if (location.isMock) {
            showAlert("Perangkat anda terdeteksi memiliki aplikasi lokasi palsu!")
            currentLocation = null
        } else {
            currentLocation = location
        }
    }

    fun getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_ACCESS_FINE_LOCATION)
        }else{

            fusedLocationClient.requestLocationUpdates(locationReq, getPendingIntent())

            fusedLocationClient.lastLocation.addOnSuccessListener {
                if(it == null){
                    showAlert("Gagal mendapatkan lokasi")
                }else{

                    if (it.isMock) {
                        showAlert("Perangkat anda terdeteksi memiliki aplikasi lokasi palsu!")
                        currentLocation = null
                    } else {
                        currentLocation = it

                        Log.d(packageName,currentLocation.toString())
                        Log.d("LAT",currentLocation!!.latitude.toString())
                        Log.d("LONG",currentLocation!!.longitude.toString())
                        if(checkLocation()) {
                            inLocation = true
                            takePicture()
                        }else{
                            inLocation = false
                            alertDialogBuilder.setTitle("Anda sedang berada di luar lokasi")
                            alertDialogBuilder.setMessage("Proses selanjutnya anda harus mengupload SPT dan memerlukan persetujuan dari admin BKD\nApakah anda ingin melanjutkan?")
                            alertDialogBuilder.setNeutralButton("Lihat Lokasi"){ dialog,_->

                                val alert = AlertDialog.Builder(this)
                                alert.setTitle("Lokasi Saya")

                                val wv = WebView(this)
                                wv.settings.javaScriptEnabled = true
                                val url = "https://maps.google.com/maps?q=${currentLocation!!.latitude},${currentLocation!!.longitude}&z=15&output=embed"

                                val data = "<iframe width='100%' height='400' src='$url' allowfullscreen frameborder='0' border='0' referrerpolicy='no-referrer-when-downgrade'></iframe>"
                                wv.loadData(data, "text/html", "UTF-8")
                                wv.webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                                        view.loadUrl(url)
                                        return true
                                    }
                                }

                                alert.setView(wv)

                                alert.setNeutralButton("Refresh") { dialog, _ ->
                                    dialog.dismiss()
                                    getLocation()
                                }

                                alert.setPositiveButton("Lanjut") { dialog, id ->
                                    dialog.dismiss()
                                    takePicture()
                                }

                                alert.setNegativeButton("Batal") { dialog, id ->
                                    dialog.dismiss()
                                }

                                alert.show()
                            }
                            alertDialogBuilder.setPositiveButton("Ya"){dialog,_->
                                dialog.dismiss()
                                takePicture()
        //                        Toast.makeText(applicationContext,"Ok",Toast.LENGTH_LONG).show()
                            }
                            alertDialogBuilder.setNegativeButton("Tidak"){dialog,_->
        //                        Toast.makeText(applicationContext,"No",Toast.LENGTH_LONG).show()
                                dialog.dismiss()
                            }
                            alertDialogBuilder.show()
                        }
                    }
                }

            }

            fusedLocationClient.lastLocation.addOnFailureListener {
                showAlert("Gagal mendapatkan lokasi")
//                Toast.makeText(applicationContext,"Gagal mendapatkan lokasi",Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun takePicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA),
                    REQUEST_IMAGE_CAPTURE)
            } else {
                dispatchTakePictureIntent()
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ACCESS_FINE_LOCATION) {
            when (grantResults[0]) {
                PackageManager.PERMISSION_GRANTED -> getLocation()
                PackageManager.PERMISSION_DENIED -> Toast.makeText(this,"Permintaan Lokasi Ditolak",Toast.LENGTH_LONG).show()
            }
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(this,"Permintaan Kamera Ditolak",Toast.LENGTH_LONG).show()
                showAlert("Permintaan Kamera Ditolak")
            } else {
                dispatchTakePictureIntent()
                Toast.makeText(this,"Permintaan Kamera Diterima",Toast.LENGTH_LONG).show()
            }
        }
    }

    fun checkIfExists() {
        mApiInterface.checkIfExists(userLoggedIn.getString("token",null)!!,userLoggedIn.getString("employee_id",null)!!,selectedWorktimeId).enqueue(object : Callback<Any>{
            override fun onResponse(call: Call<Any>?, response: Response<Any>) {
                if(response.code() == 200){
                    Log.d(packageName, response.body().toString())
                    var res = Gson().toJsonTree(response.body()).asJsonObject
                    var data = res.get("data").asBoolean
                    if(data){
                        showAlert("Maaf! Anda sudah melakukan absen")
                    }else{
                        getLocation()
                    }
                }
                Log.d(packageName, response.raw().toString())
            }

            override fun onFailure(call: Call<Any>?, t: Throwable?) {
                Log.d(packageName, t.toString())
                showAlert("Ada Kesalahan Server")
            }
        })
    }

    fun openAbsenTeman(){
        val dialog = Dialog(this)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.absen_teman)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        var nip = dialog.findViewById(R.id.nip) as TextInputLayout
        val yesBtn = dialog.findViewById(R.id.submit) as Button
        val noBtn = dialog.findViewById(R.id.cancel) as Button

        yesBtn.setOnClickListener {
            profileByNip(nip.editText?.text.toString())
            dialog.dismiss()
        }

        noBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()

    }

    fun openCuti(){

        val dialog = Dialog(this)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.cuti)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        var btnWaktuMulai = dialog.findViewById(R.id.btn_waktu_mulai) as Button
        var btnWaktuSelesai = dialog.findViewById(R.id.btn_waktu_selesai) as Button
        var btnLampiran = dialog.findViewById(R.id.btn_lampiran) as Button
        lampiranCutiText = dialog.findViewById(R.id.tv_lampiran) as TextView
        lampiranCutiText.text = "Pilih File Lebih Dahulu"

        var tvWaktuMulai = dialog.findViewById(R.id.tv_waktu_mulai) as TextView
        var tvWaktuSelesai = dialog.findViewById(R.id.tv_waktu_selesai) as TextView

        val yesBtn = dialog.findViewById(R.id.submit) as Button
        val noBtn = dialog.findViewById(R.id.cancel) as Button

        val jenisPengajuan = dialog.findViewById(R.id.jenis_pengajuan) as Spinner
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
            CustomDatePickerDialog.show(this, dialog.findViewById(R.id.tv_waktu_mulai))
        }

        btnWaktuSelesai.setOnClickListener{
            tvWaktuSelesai.text = CustomDatePickerDialog.year.toString()+"-"+(CustomDatePickerDialog.month+1).toString()+"-01"
            CustomDatePickerDialog.show(this, dialog.findViewById(R.id.tv_waktu_selesai))
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

    fun openTugasLuar(){

        val dialog = Dialog(this)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.tugas_luar)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        var btnWaktuMulai = dialog.findViewById(R.id.btn_waktu_mulai) as Button
        var btnWaktuSelesai = dialog.findViewById(R.id.btn_waktu_selesai) as Button
        var btnLampiran = dialog.findViewById(R.id.btn_lampiran) as Button
        lampiranTugasLuarText = dialog.findViewById(R.id.tv_lampiran) as TextView
        lampiranTugasLuarText.text = "Pilih File Lebih Dahulu"

        var tvWaktuMulai = dialog.findViewById(R.id.tv_waktu_mulai) as TextView
        var tvWaktuSelesai = dialog.findViewById(R.id.tv_waktu_selesai) as TextView

        val yesBtn = dialog.findViewById(R.id.submit) as Button
        val noBtn = dialog.findViewById(R.id.cancel) as Button

        btnWaktuMulai.setOnClickListener{
            tvWaktuMulai.text = CustomDatePickerDialog.year.toString()+"-"+(CustomDatePickerDialog.month+1).toString()+"-"+CustomDatePickerDialog.day.toString()
            CustomDatePickerDialog.show(this, dialog.findViewById(R.id.tv_waktu_mulai))
        }

        btnWaktuSelesai.setOnClickListener{
            tvWaktuSelesai.text = CustomDatePickerDialog.year.toString()+"-"+(CustomDatePickerDialog.month+1).toString()+"-"+CustomDatePickerDialog.day.toString()
            CustomDatePickerDialog.show(this, dialog.findViewById(R.id.tv_waktu_selesai))
        }

        btnLampiran.setOnClickListener {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, "Select a file"), REQUEST_FILE_TUGAS_LUAR)
        }

        yesBtn.setOnClickListener {
            if(attachmentUri != null && tvWaktuMulai.text.toString() != null && tvWaktuSelesai.text.toString() != null){
                presences("tugas luar",attachmentUri!!,tvWaktuMulai.text.toString(),tvWaktuSelesai.text.toString())
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

    fun profileByNip(nip:String) {
        mApiInterface.profileByNip(userLoggedIn.getString("token",null)!!,nip).enqueue(object : Callback<Any>{
            override fun onResponse(call: Call<Any>?, response: Response<Any>) {
                if(response.code() == 200) {
                    Log.d(packageName, response.body().toString())
                    var res = Gson().toJsonTree(response.body()).asJsonObject
                    var data = res.getAsJsonObject("data")
                    if(data != null){
                        var is_android_user = data.get("is_android_user").asInt
                        if(is_android_user == 1){
                            showAlert("Maaf! Pegawai dengan NIP ${nip} adalah pengguna Android")
                        }else{
                            absenTemanId = data.get("id").asString
                            binding.absen.performClick()
                        }
                    }else {
                        showAlert("Data Tidak Ditemukan!")
                    }
                }else{
                    var jsonObject: JSONObject? = null
                    try {
                        jsonObject = JSONObject(response.errorBody().string())
                        val message: String = jsonObject.getString("message")
                        Log.d(packageName,message)
//                        showAlert(message)
//                        Toast.makeText(applicationContext,message,Toast.LENGTH_LONG).show()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
                Log.d(packageName, response.raw().toString())
            }

            override fun onFailure(call: Call<Any>?, t: Throwable?) {
                Log.d(packageName, t.toString())
                showAlert("Ada Kesalahan Server")
            }
        })
    }

    override fun onClick(view: View?) {
        when(view?.id){
            binding.absen.id->{
                if(isMockSettingsON()){
                    showAlert("Perangkat anda terdeteksi memiliki aplikasi lokasi palsu!")
                }else{
                    getWorktime()
                }
            }
            binding.profil.id->{
                startActivity(Intent(applicationContext, ProfileActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
                loading.dismiss()
            }
            binding.history.id->{
                startActivity(Intent(applicationContext, HistoryActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
                loading.dismiss()
            }
            binding.historyCuti.id->{
                var intent = Intent(applicationContext, HistoryActivity::class.java)
                intent.putExtra("type","cuti")
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
                loading.dismiss()
            }
            binding.historyTugasLuar.id->{
                var intent = Intent(applicationContext, HistoryActivity::class.java)
                intent.putExtra("type","tugas luar")
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
                loading.dismiss()
            }
            binding.izinKerja.id->{
                val intent = Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT)

                startActivityForResult(Intent.createChooser(intent, "Select a file"), REQUEST_FILE_IZIN)
            }
            binding.sakit.id->{
                val intent = Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT)

                startActivityForResult(Intent.createChooser(intent, "Select a file"), REQUEST_FILE_SAKIT)
            }
            binding.absenTeman.id->{
                openAbsenTeman()
            }
            binding.cuti.id->{
                openCuti()
            }
            binding.tugasLuar.id->{
                openTugasLuar()
            }
            binding.logout.id->{
                userLoggedIn.edit().clear().apply()
                startActivity(Intent(applicationContext, SplashScreenActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
                loading.dismiss()
                finish()
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try{
            photoFile = getPhotoFile(FILE_NAME)
            val fileProvider = FileProvider.getUriForFile(applicationContext, "${BuildConfig.APPLICATION_ID}.fileprovider", photoFile)
            photoUri = fileProvider
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }catch(err:Exception){
            showAlert("Tidak bisa buka kamera")
//            Toast.makeText(this, "Tidak bisa buka kamera", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            var uri = resizeImage(photoUri)
            presences("hadir", uri)
        }

        if(requestCode == REQUEST_FILE_IZIN && resultCode == RESULT_OK) {
            if(data !=null){
                var selectedfile: Uri = data.data!!
                presences("izin", uriFile = selectedfile)
            }
        }

        if(requestCode == REQUEST_FILE_TUGAS_LUAR && resultCode == RESULT_OK) {
            if(data !=null){
                attachmentUri = data.data!!
                lampiranTugasLuarText.text  = getFileName(attachmentUri!!)
            }
        }

        if(requestCode == REQUEST_FILE_SAKIT && resultCode == RESULT_OK) {
            if(data !=null){
                var selectedfile: Uri = data.data!!
                presences("sakit", selectedfile)
            }
        }

        if(requestCode == REQUEST_FILE_CUTI && resultCode == RESULT_OK) {
            if(data !=null){
                attachmentUri = data.data!!
                lampiranCutiText.text  = getFileName(attachmentUri!!)
            }
        }

        if(requestCode == REQUEST_FILE_LUAR_LOKASI && resultCode == RESULT_OK) {
            if(data !=null){
                attachmentUri = data.data!!
                luarLokasiText.text  = getFileName(attachmentUri!!)
                luarLokasiText.visibility = View.VISIBLE
//                presences("sakit", selectedfile)
            }
        }
    }

    private fun getPhotoFile(fileName: String): File {
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDirectory)
    }

    fun checkLocation():Boolean{

        for (i in 0 until places.length()) {

            var place = places.getJSONObject(i)
            var newLoc = Location("")
            newLoc.latitude = place.getString("lat").replace(",",".").toDouble()
            newLoc.longitude = place.getString("lng").replace(",",".").toDouble()
            var distance = LocationDistance.betweenCoordinates(currentLocation!!,newLoc)

            if(distance <= radius!!){
                return true
            }
        }

        return false
    }

    fun isMockSettingsON(): Boolean = if (Settings.Secure.getString(contentResolver, Settings.Secure.ALLOW_MOCK_LOCATION).equals("0")) false else true

    private fun resizeImage(_uri: Uri) : Uri {
        var _bitmap = MediaStore.Images.Media.getBitmap(contentResolver, _uri)
        var afd = contentResolver.openAssetFileDescriptor(_uri, "r")
        var fileSize: Long = afd!!.length
        afd.close()
        Log.d(packageName, "SIZE ORIGINAL: $fileSize")

        var bitmap = _bitmap
        Log.d(packageName, "SIZE BITMAP : ${bitmap.width}")
//        if (bitmap.width > 1000) {
//            val divider = bitmap.width / 1000
//            val width = 1000
//            val height = bitmap.height / divider
//            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
//        }

        Log.d(packageName, "SIZE NEW BITMAP : ${bitmap.width}")

        val imageOut = contentResolver.openOutputStream(_uri)
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, imageOut)
            Log.d(packageName, "SIZE NEW BITMAP HERE : ${bitmap.width}")
        } finally {
            imageOut!!.close()
        }

        afd = contentResolver.openAssetFileDescriptor(_uri, "r")
        fileSize = afd!!.length
        afd.close()
        Log.d(packageName, "SIZE RESIZED : $fileSize")
        return _uri
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

    private fun viewMap() {
        Log.i(packageName, "${currentLocation?.longitude}, ${currentLocation?.latitude}")

        val gmmIntentUri = Uri.parse("google.streetview:cbll=${currentLocation?.latitude},${currentLocation?.longitude}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent)
    }

    fun presences(type:String, uriFile: Uri, waktuMulai : String? = "", waktuSelesai: String? = ""){
        loading.show()

        var pic_url: MultipartBody.Part? = null
        if(type == "hadir"){
            var inputStream = contentResolver.openInputStream(uriFile)

            var imageData = inputStream?.buffered()?.use { it.readBytes() }
            var requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), imageData)
            pic_url = MultipartBody.Part.createFormData("pic",FILE_NAME, requestFile)
        }else{
            pic_url = MultipartBody.Part.createFormData("pic","")
        }

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
        val lngBody = RequestBody.create(MediaType.parse("multipart/form-data"), if(currentLocation != null) currentLocation?.longitude.toString() else "")
        val latBody = RequestBody.create(MediaType.parse("multipart/form-data"), if(currentLocation != null) currentLocation?.latitude.toString() else "")
        val inLocationBody = RequestBody.create(MediaType.parse("multipart/form-data"), if(inLocation) inLocation.toString() else "")
        val worktimeItemId = RequestBody.create(MediaType.parse("multipart/form-data"),selectedWorktimeId)
        val startedAt = RequestBody.create(MediaType.parse("multipart/form-data"),waktuMulai)
        val finishedAt = RequestBody.create(MediaType.parse("multipart/form-data"),waktuSelesai)

        var employee_id = if(absenTemanId != "") absenTemanId else userLoggedIn.getString("employee_id",null)

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
//                    showAlert("Kirim Data Berhasil!")
                    attachmentUri = null
                    absenTemanId = ""

                    var res = Gson().toJsonTree(response.body()).asJsonObject
                    var data = res.getAsJsonObject("data")
                    var alert = AlertDialog.Builder(this@MainActivity)
                    alert.setMessage("Kirim Data Berhasil!")
                    alert.setPositiveButton("Ok"){dialog,_->
                        dialog.dismiss()
                        var intent = Intent(this@MainActivity, HistoryDetailActivity::class.java)
                        intent.putExtra("employee_presence_id", data.get("id").asString)
                        startActivity(intent)
                    }
                    alert.show()

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

    fun locationRequest() {
        locationReq = LocationRequest()
        locationReq!!.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationReq!!.setInterval(1500)
        locationReq!!.setFastestInterval(750)
        locationReq!!.setSmallestDisplacement(10f)
    }

    fun getPendingIntent(): PendingIntent? {
        val intent = Intent(this, GpsService::class.java)
        intent.action = "1"
        return PendingIntent.getBroadcast(this, 0, intent,  PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver)
        }
    }
}