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
            binding.atasan.text = data.atasan
            binding.namaAtasan.text = data.namaAtasan
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
                var res = Gson().toJsonTree(response.body()).asJsonObject
                var data = res.getAsJsonObject("data")

                var profile = Profile()
                profile.nama = data.get("name").asString
                profile.nip = data.get("nip").asString
                profile.golongan = data.get("group").asString
                profile.jabatan = data.get("position").asString
                profile.instansi = data.getAsJsonObject("workunit").get("name").asString
                profile.atasan = if(data.get("head_position") != null) data.get("head_position").asString else "-"
                profile.namaAtasan = if(data.get("head_name") != null) data.get("head_name").asString else "-"
                profile.ponsel = data.get("phone").asString

                mainViewModel.profile.postValue(profile)
            }

            override fun onFailure(call: Call<Any>, t: Throwable) {
                Log.d(packageName, t.toString())
            }
        })
    }

}