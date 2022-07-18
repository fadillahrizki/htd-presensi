package com.htd.presensi.activity

import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.htd.presensi.R
import com.htd.presensi.databinding.ActivityProfileBinding
import com.htd.presensi.models.Profile
import com.htd.presensi.rest.ApiClient
import com.htd.presensi.rest.ApiInterface
import com.htd.presensi.util.Loading
import com.htd.presensi.viewmodel.MainViewModel
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class ProfileActivity : AppCompatActivity(){
    lateinit var binding: ActivityProfileBinding
    private lateinit var userLoggedIn: SharedPreferences
    lateinit var mApiInterface: ApiInterface

    var mainViewModel: MainViewModel = MainViewModel()

    lateinit var loading:Loading

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
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        userLoggedIn = getSharedPreferences("login_data", MODE_PRIVATE)
        mApiInterface = ApiClient.client!!.create(ApiInterface::class.java)

        supportActionBar?.title = "Profil"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        loading = Loading(this)
    }

    fun observe(){
        mainViewModel.profile.observe(this){ data ->
            binding.nama.text = data.nama
            binding.nip.text = data.nip
            binding.golongan.text = data.golongan
            binding.jabatan.text = data.jabatan
            binding.instansi.text = data.instansi
//            binding.atasan.text = data.atasan
//            binding.namaAtasan.text = data.namaAtasan
            binding.ponsel.text = data.ponsel

            binding.container.visibility = View.VISIBLE
            loading.hide()
        }
    }

    fun api(){
        getData()
    }

    fun listener(){

    }

    fun getData() {
        loading.show()
        mApiInterface.profile(userLoggedIn.getString("token",null)!!,userLoggedIn.getString("employee_id",null)!!).enqueue(object : Callback<Any> {
            override fun onResponse(
                call: Call<Any>,
                response: Response<Any>
            ) {
                if(response.code() == 200) {
                    Log.d(packageName, response.body().toString())
                    var res = Gson().toJsonTree(response.body()).asJsonObject
                    var data = res.getAsJsonObject("data")

                    var profile = Profile()
                    profile.nama = if (data.get("name") != null) data.get("name").asString else "-"
                    profile.nip = if (data.get("nip") != null) data.get("nip").asString else "-"
                    profile.golongan =
                        if (data.get("group") != null) data.get("group").asString else "-"
                    profile.jabatan =
                        if (data.get("position") != null) data.get("position").asString else "-"
                    profile.instansi =
                        if (data.get("workunit") != null) data.getAsJsonObject("workunit")
                            .get("name").asString else "-"
                    profile.atasan =
                        if (data.get("head_position") != null) data.get("head_position").asString else "-"
                    profile.namaAtasan =
                        if (data.get("head_name") != null) data.get("head_name").asString else "-"
                    profile.ponsel =
                        if (data.get("phone") != null) data.get("phone").asString else "-"

                    mainViewModel.profile.postValue(profile)
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
            }

            override fun onFailure(call: Call<Any>, t: Throwable) {
                Log.d(packageName, t.toString())
            }
        })
    }

}