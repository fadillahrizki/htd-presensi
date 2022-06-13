package com.htd.presensi.activity

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.htd.presensi.BuildConfig
import com.htd.presensi.R
import com.htd.presensi.databinding.ActivityMainBinding
import com.htd.presensi.rest.ApiClient
import com.htd.presensi.rest.ApiInterface
import com.htd.presensi.util.Loading
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class MainActivity : AppCompatActivity(), View.OnClickListener {

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

    fun getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_ACCESS_FINE_LOCATION)
        }else{
            fusedLocationClient.lastLocation.addOnSuccessListener { location : Location? ->
                Toast.makeText(this,"Long: ${location?.longitude}\nLat: ${location?.latitude}",Toast.LENGTH_LONG).show()
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
                takePicture()
//                getLocation()
            }
            binding.profil.id->{
                startActivity(Intent(applicationContext, ProfileActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
            }
            binding.history.id->{
                startActivity(Intent(applicationContext, HistoryActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
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
            binding.image.setImageURI(photoUri)
            presences("hadir", photoUri)
        }

        if(requestCode == REQUEST_FILE_IZIN && resultCode == RESULT_OK) {
            if(data !=null){
                var selectedfile: Uri = data.data!!
                binding.image.setImageURI(selectedfile)
                presences("izin", uriFile = selectedfile)
            }
        }

        if(requestCode == REQUEST_FILE_SAKIT && resultCode == RESULT_OK) {
            if(data !=null){
                var selectedfile: Uri = data.data!!
                binding.image.setImageURI(selectedfile)
                presences("sakit", selectedfile)
            }
        }
    }

    private fun getPhotoFile(fileName: String): File {
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDirectory)
    }

    fun presences(type:String,uriFile: Uri){
        loading.show()

        var inputStream = contentResolver.openInputStream(uriFile)
        var imageData = inputStream?.buffered()?.use { it.readBytes() }
        var requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), imageData)
        var attachment = MultipartBody.Part.createFormData("attachment",FILE_NAME, requestFile)
        val typeBody: RequestBody = RequestBody.create(MediaType.parse("multipart/form-data"), type)

        mApiInterface.presences(userLoggedIn.getString("token",null)!!,"1",typeBody, attachment).enqueue(object : Callback<Any> {
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