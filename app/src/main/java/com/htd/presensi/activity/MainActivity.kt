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
import com.google.gson.Gson
import com.htd.presensi.BuildConfig
import com.htd.presensi.R
import com.htd.presensi.databinding.ActivityMainBinding
import com.htd.presensi.models.WorktimeItem
import com.htd.presensi.rest.ApiClient
import com.htd.presensi.rest.ApiInterface
import com.htd.presensi.services.GPS_Service
import com.htd.presensi.services.GpsService
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


class MainActivity : AppCompatActivity(), View.OnClickListener,LocationListener,
    AdapterView.OnItemSelectedListener {

    lateinit var binding: ActivityMainBinding
    lateinit var mApiInterface: ApiInterface
    lateinit var userLoggedIn: SharedPreferences
    lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var photoFile: File
    lateinit var photoUri: Uri
    lateinit var loading: Loading
    lateinit var places : JSONArray
    lateinit var alertDialogBuilder : AlertDialog.Builder
    lateinit var luarLokasiBtn : Button
    lateinit var luarLokasiText : TextView

    lateinit var worktimeSpinner: Spinner

    lateinit var worktimesAdapter: ArrayAdapter<String>
    var arrWorktimeItems: ArrayList<String> = ArrayList()
    var arrWorktimeItemIds: ArrayList<String> = ArrayList()
    var selectedWorktime = ""
    var selectedWorktimeId = ""

    val REQUEST_IMAGE_CAPTURE = 1
    val REQUEST_FILE_IZIN = 2
    val REQUEST_FILE_SAKIT = 3
    val REQUEST_FILE_LUAR_LOKASI = 4
    val PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 100
    val FILE_NAME = "presence.jpg"
    var currentLocation: Location? = null
    var inLocation = true
    var radius: Double? = 0.0
    var luarLokasiUri : Uri? = null

    var mainViewModel: MainViewModel = MainViewModel()

    var counts = emptyArray<Int>()
    var time = 0
    var count = "00:00:00"

    lateinit var worktimeDialog : Dialog

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

    private fun showDialog() {
        val dialog = Dialog(this)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.out_location)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        luarLokasiBtn = dialog.findViewById(R.id.attachent_luar) as Button
        luarLokasiText = dialog.findViewById(R.id.attachent_luar_text) as TextView
        val yesBtn = dialog.findViewById(R.id.submit) as Button
        val noBtn = dialog.findViewById(R.id.cancel) as Button

        luarLokasiBtn.setOnClickListener {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, "Select a file"), REQUEST_FILE_LUAR_LOKASI)
        }

        yesBtn.setOnClickListener {
            takePicture()
            dialog.dismiss()
        }

        noBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()

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

//        if(userLoggedIn.getString("role",null) == "pegawai"){
//            binding.absen.visibility = View.VISIBLE
//            binding.history.visibility = View.VISIBLE
//        }else if(userLoggedIn.getString("role",null) == "kasubagumum"){
//            binding.izinKerja.visibility = View.VISIBLE
//            binding.sakit.visibility = View.VISIBLE
//        }

//        worktimeDialog = Dialog(this)
//        worktimeDialog.setContentView(R.layout.worktime)
//        worktimeDialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//        worktimeSpinner = worktimeDialog.findViewById(R.id.worktime_spinner) as Spinner

        binding.name.text = "Hai, "+userLoggedIn.getString("name",null)

        if(radius == null){
            binding.absen.visibility = View.GONE
        }
    }

    fun observe(){
//        mainViewModel.activeWorktime.observe(this){data->
//            if(data != null){
//                arrWorktimeItems.add("- Pilih -")
//                arrWorktimeItemIds.add("0")
//                for(item in data) {
//                    arrWorktimeItems.add(item.name!!)
//                    arrWorktimeItemIds.add(item.id!!)
//                }
//
//                worktimesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrWorktimeItems)
//                worktimesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//                worktimeSpinner.adapter = worktimesAdapter
//
//            }
//        }

        mainViewModel.activeWorktime.observe(this){data->
            if(data != null){
                selectedWorktimeId = data.id!!
                checkIfExists()
            }else{
                showAlert("Maaf! Sekarang sedang tidak ada jadwal absensi")
            }
        }

        mainViewModel.times.observe(this){data->
            count = data
        }

        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                counts = count.split(':').map{it.toInt()}.toTypedArray()
                time = counts[2]
                time += counts[1] * 60
                time += (counts[0] * 60) * 60

                if (counts[1] >= 60 && counts[0] > 0) {
                    counts[0] = counts[0] + 1
                    counts[1] = 1
                }

                if (counts[2] >= 60 && counts[1] > 0) {
                    counts[1] = counts[1] + 1
                    counts[2] = 1
                }

                if (counts[0] >= 60) {
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
                            showAlert("Maaf! Sekarang sedang tidak ada jadwal absensi")
                        }
                    }
                }else{
                    var jsonObject: JSONObject? = null
                    try {
                        jsonObject = JSONObject(response.errorBody().string())
                        val message: String = jsonObject.getString("message")
                        showAlert("Maaf! Sekarang sedang tidak ada jadwal absensi")
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

    fun listener(){
        binding.absen.setOnClickListener(this)
        binding.profil.setOnClickListener(this)
        binding.history.setOnClickListener(this)
        binding.izinKerja.setOnClickListener(this)
        binding.sakit.setOnClickListener(this)
        binding.logout.setOnClickListener(this)
//        worktimeSpinner.setOnItemSelectedListener(this)
//
//        (worktimeDialog.findViewById(R.id.ok) as Button).setOnClickListener {
//            if(selectedWorktimeId != "0"){
//                worktimeDialog.dismiss()
//                getLocation()
//            }else{
//                showAlert("Pilih Absen lebih dahulu!")
//            }
//        }
//
//        (worktimeDialog.findViewById(R.id.cancel) as Button).setOnClickListener {
//            worktimeDialog.dismiss()
//        }
    }


    override fun onItemSelected(p0: AdapterView<*>?, view: View?, idx: Int, p3: Long) {
        when(p0?.id){
//            worktimeSpinner.id -> {
//                selectedWorktime = arrWorktimeItems[idx]
//                selectedWorktimeId = arrWorktimeItemIds[idx]
////                worktimeDialog.dismiss()
//            }
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
    }

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 100
    }

    fun getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_ACCESS_FINE_LOCATION)
        }else{

            fusedLocationClient.lastLocation.addOnSuccessListener {
                if(it == null){
                    showAlert("Gagal mendapatkan lokasi")
                }else{
                    currentLocation=it
                    Log.d(packageName,currentLocation.toString())
                    Log.d("LAT",currentLocation!!.latitude.toString())
                    Log.d("LONG",currentLocation!!.longitude.toString())
                    if(checkLocation()) {
                        takePicture()
                    }else{
                        inLocation = false
                        alertDialogBuilder.setTitle("Anda sedang di luar lokasi")
                        alertDialogBuilder.setMessage("Apakah anda ingin melanjutkan ?")
                        alertDialogBuilder.setPositiveButton("Ya"){dialog,_->
                            takePicture()
    //                        showDialog()
    //                        Toast.makeText(applicationContext,"Ok",Toast.LENGTH_LONG).show()
                        }
                        alertDialogBuilder.setNegativeButton("Tidak"){dialog,_->
    //                        Toast.makeText(applicationContext,"No",Toast.LENGTH_LONG).show()
                        }
                        alertDialogBuilder.show()
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

        if(requestCode == REQUEST_FILE_SAKIT && resultCode == RESULT_OK) {
            if(data !=null){
                var selectedfile: Uri = data.data!!
                presences("sakit", selectedfile)
            }
        }

        if(requestCode == REQUEST_FILE_LUAR_LOKASI && resultCode == RESULT_OK) {
            if(data !=null){
                luarLokasiUri = data.data!!
                luarLokasiText.text  = getFileName(luarLokasiUri!!)
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

    fun presences(type:String, uriFile: Uri){
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
        if(luarLokasiUri != null){
            var isLuar = contentResolver.openInputStream(luarLokasiUri!!)

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

        mApiInterface.presences(userLoggedIn.getString("token",null)!!,userLoggedIn.getString("employee_id",null)!!,typeBody,attachment,latBody,lngBody,inLocationBody,pic_url,worktimeItemId).enqueue(object : Callback<Any> {
            override fun onResponse(
                call: Call<Any>,
                response: Response<Any>
            ) {
                if(response.code() == 200){
                    Log.d(packageName, response.body().toString())
                    Toast.makeText(applicationContext,"Berhasil",Toast.LENGTH_LONG).show()
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

    fun showUpdateLocation(txt: String?) {
        Toast.makeText(this, txt, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver)
        }
    }
}