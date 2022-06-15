package com.htd.presensi.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.*
import com.htd.presensi.BuildConfig
import com.htd.presensi.R
import com.htd.presensi.databinding.ActivityMainBinding
import com.htd.presensi.rest.ApiClient
import com.htd.presensi.rest.ApiInterface
import com.htd.presensi.util.CurrencyFormat.format
import com.htd.presensi.util.Loading
import com.htd.presensi.util.LocationDistance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity(), View.OnClickListener,LocationListener {

    lateinit var binding: ActivityMainBinding
    lateinit var mApiInterface: ApiInterface
    lateinit var userLoggedIn: SharedPreferences
    lateinit var fusedLocationClient: FusedLocationProviderClient

    val REQUEST_IMAGE_CAPTURE = 1
    val REQUEST_FILE_IZIN = 2
    val REQUEST_FILE_SAKIT = 3
    val PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 100
    val FILE_NAME = "presence.jpg"

    private lateinit var photoFile: File
    private lateinit var photoUri: Uri

    lateinit var loading: Loading
    var currentLocation: Location? = null

    lateinit var places : JSONArray

    lateinit var alertDialogBuilder : AlertDialog.Builder
    var inLocation = true

    var radius: Double? = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        init()
        observe()
        api()
        listener()
    }

    fun init(){
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        mApiInterface = ApiClient.client!!.create(ApiInterface::class.java)
        userLoggedIn = getSharedPreferences("login_data", MODE_PRIVATE)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        loading = Loading(this)

        places = JSONTokener(userLoggedIn.getString("places",null)).nextValue() as JSONArray

        alertDialogBuilder = AlertDialog.Builder(this)

        radius = userLoggedIn.getString("radius",null)?.toDouble()

        if(radius == null){
            binding.absen.visibility = View.GONE
        }

    }

    fun observe(){

    }

    fun api(){
    }

    fun listener(){
        binding.absen.setOnClickListener(this)
        binding.profil.setOnClickListener(this)
        binding.history.setOnClickListener(this)
        binding.izinKerja.setOnClickListener(this)
        binding.sakit.setOnClickListener(this)
        binding.logout.setOnClickListener(this)
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
    }

    fun getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_ACCESS_FINE_LOCATION)
        }else{

            fusedLocationClient.lastLocation.addOnSuccessListener {
                currentLocation=it
                    if(checkLocation()) {
                        takePicture()
                    }else{
                        inLocation = false
                        alertDialogBuilder.setTitle("Anda sedang di luar lokasi")
                        alertDialogBuilder.setMessage("Apakah anda ingin melanjutkan ?")
                        alertDialogBuilder.setPositiveButton("Ya"){dialog,_->
                            takePicture()
    //                        Toast.makeText(applicationContext,"Ok",Toast.LENGTH_LONG).show()
                        }
                        alertDialogBuilder.setNegativeButton("Tidak"){dialog,_->
    //                        Toast.makeText(applicationContext,"No",Toast.LENGTH_LONG).show()
                        }
                        alertDialogBuilder.show()
                    }

            }

            fusedLocationClient.lastLocation.addOnFailureListener {
                Toast.makeText(applicationContext,"Gagal mendapatkan lokasi",Toast.LENGTH_LONG).show()
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
                Toast.makeText(this,"Permintaan Kamera Ditolak",Toast.LENGTH_LONG).show()
            } else {
                dispatchTakePictureIntent()
                Toast.makeText(this,"Permintaan Kamera Diterima",Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onClick(view: View?) {
        when(view?.id){
            binding.absen.id->{
                getLocation()
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
            Toast.makeText(this, "Tidak bisa buka kamera", Toast.LENGTH_SHORT).show()
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
    }

    private fun getPhotoFile(fileName: String): File {
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDirectory)
    }

    fun checkLocation():Boolean{

        for (i in 0 until places.length()) {

            var place = places.getJSONObject(i)
            var newLoc = Location("")
            newLoc.latitude = place.getString("lat").toDouble()
            newLoc.longitude = place.getString("lng").toDouble()
            Log.d(packageName,newLoc.latitude.toString())
            var distance = LocationDistance.betweenCoordinates(currentLocation!!,newLoc)

            if(distance <= radius!!){
                return true
            }
        }

        return false
    }

    private fun resizeImage(_uri: Uri) : Uri {
        var _bitmap = MediaStore.Images.Media.getBitmap(contentResolver, _uri)
        var afd = contentResolver.openAssetFileDescriptor(_uri, "r")
        var fileSize: Long = afd!!.length
        afd.close()
        Log.d(packageName, "SIZE ORIGINAL: $fileSize")

        var bitmap = _bitmap
        Log.d(packageName, "SIZE BITMAP : ${bitmap.width}")
        if (bitmap.width > 100) {
            val divider = bitmap.width / 100
            val width = 100
            val height = bitmap.height / divider
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
        }

        Log.d(packageName, "SIZE NEW BITMAP : ${bitmap.width}")

        val imageOut = contentResolver.openOutputStream(_uri)
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, imageOut)
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

    fun presences(type:String, uriFile: Uri){
        loading.show()

        var inputStream = contentResolver.openInputStream(uriFile)

        var imageData = inputStream?.buffered()?.use { it.readBytes() }
        var requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), imageData)
        var attachment = MultipartBody.Part.createFormData("attachment",FILE_NAME, requestFile)

        val typeBody: RequestBody = RequestBody.create(MediaType.parse("multipart/form-data"), type)
        val lngBody = RequestBody.create(MediaType.parse("multipart/form-data"), if(currentLocation != null) currentLocation?.longitude.toString() else "")
        val latBody = RequestBody.create(MediaType.parse("multipart/form-data"), if(currentLocation != null) currentLocation?.latitude.toString() else "")
        val inLocationBody = RequestBody.create(MediaType.parse("multipart/form-data"), if(inLocation) inLocation.toString() else "")

        mApiInterface.presences(userLoggedIn.getString("token",null)!!,userLoggedIn.getString("employee_id",null)!!,typeBody,attachment,lngBody,latBody,inLocationBody).enqueue(object : Callback<Any> {
            override fun onResponse(
                call: Call<Any>,
                response: Response<Any>
            ) {
                if(response.code() == 200){
                    Toast.makeText(applicationContext,"Berhasil",Toast.LENGTH_LONG).show()
                }else{
                    var jsonObject: JSONObject? = null
                    try {
                        jsonObject = JSONObject(response.errorBody().string())
                        val message: String = jsonObject.getString("message")
                        Toast.makeText(applicationContext,message,Toast.LENGTH_LONG).show()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
                Log.d(packageName, response.raw().toString())
                loading.hide()
            }

            override fun onFailure(call: Call<Any>, t: Throwable) {
                Log.d(packageName, t.toString())
                Toast.makeText(applicationContext,"Ada Kesalahan Server",Toast.LENGTH_LONG).show()
                loading.hide()
            }
        })

    }
}