package com.htd.presensi.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings.Secure
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.htd.presensi.R
import com.htd.presensi.databinding.ActivityLoginBinding
import com.htd.presensi.rest.ApiClient
import com.htd.presensi.rest.ApiInterface
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.security.AccessController.getContext


class LoginActivity : AppCompatActivity(), View.OnClickListener {
    lateinit var binding: ActivityLoginBinding
    lateinit var mApiInterface: ApiInterface
    var REQUEST_PERMISSIONS = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())
        mApiInterface = ApiClient.client!!.create(ApiInterface::class.java)
        binding.btnLogin.setOnClickListener(this)
        requestPermissions()
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnLogin.id -> login()
        }
    }

    fun requestPermissions(){
        var pers = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA)

        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, pers, REQUEST_PERMISSIONS)
        }
    }

    private fun login() {
        var allTrue = true
        if (binding.username.editText?.text.toString().isEmpty()) {
            binding.username.error = "Username tidak boleh kosong"
            allTrue = false
        }
        if (binding.password.editText?.text.toString().isEmpty()) {
            binding.password.error = "Kata Sandi tidak boleh kosong"
            allTrue = false
        }
        if (allTrue) {
            binding.btnLogin.setText("Loading...")
            val loginData = getSharedPreferences("login_data", MODE_PRIVATE)
            val username: String = binding.username.editText?.text.toString()
            val password: String = binding.password.editText?.text.toString()

            val deviceNumber = Secure.getString(
                contentResolver,
                Secure.ANDROID_ID
            )

            mApiInterface.login(username, password,deviceNumber).enqueue(object : Callback<Any> {
                override fun onResponse(
                    call: Call<Any>,
                    response: Response<Any>
                ) {
                    Log.d(packageName,response.raw().toString())

                    if (response.code() != 200) {
                        binding.cardError.setVisibility(View.VISIBLE)
                        binding.tvError.setText("Username / Password tidak sesuai!")

                        var jsonObject: JSONObject? = null
                        try {
                            jsonObject = JSONObject(response.errorBody().string())
                            Log.d(packageName,jsonObject.toString())
                            val message: String = jsonObject.getString("message")
                            if(response.code() == 401){
                                binding.tvError.setText(message)
                            }else{
                                Toast.makeText(applicationContext,message, Toast.LENGTH_LONG).show()
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {

                        var res = Gson().toJsonTree(response.body()).asJsonObject
                        val editor = loginData.edit()
                        val data = res.get("data").asJsonObject
                        val userData = data.get("user").asJsonObject
                        val employeeData = userData.get("employee").asJsonObject

                        editor.putString("id", userData.get("id").asString)
                        editor.putString("name", employeeData.get("name").asString)
                        editor.putString("email", userData.get("email").asString)
                        editor.putString("password", password)
                        editor.putString("role", userData.get("role").asString)
                        editor.putString("radius", userData.get("radius")?.asString)
                        editor.putString("employee_id", userData.get("employee_id").asInt.toString())
                        editor.putString("token", data.get("token").asString)

                        editor.putString("places",userData.getAsJsonArray("places").toString())

                        editor.apply()

                        Log.d(packageName,userData.toString())

                        binding.cardError.setVisibility(View.GONE)
                        val intent = Intent(applicationContext, SplashScreenActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        finish()
                    }
                    binding?.btnLogin.setText("Masuk")
                }

                override fun onFailure(call: Call<Any>, t: Throwable) {
                    Log.d(packageName, t.toString())
                    Toast.makeText(applicationContext,"Ada Kesalahan Server",Toast.LENGTH_LONG).show()
                    binding.btnLogin.setText("Masuk")
                }
            })
        }
    }
}
