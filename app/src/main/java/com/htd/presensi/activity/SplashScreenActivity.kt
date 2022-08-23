package com.htd.presensi.activity

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.htd.presensi.R
import com.htd.presensi.databinding.ActivitySplashScreenBinding
import com.htd.presensi.rest.ApiClient
import com.htd.presensi.rest.ApiInterface
import com.htd.presensi.util.DetectConnection.checkInternetConnection
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SplashScreenActivity : AppCompatActivity() {
    private lateinit var userLoggedIn: SharedPreferences
    private lateinit var binding: ActivitySplashScreenBinding
    lateinit var mApiInterface: ApiInterface

    fun getDeviceName(): String? {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            model.capitalize()
        } else {
            ((manufacturer).toString() + " " + model).capitalize()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        Log.d("HP", getDeviceName()!!)
        userLoggedIn = getSharedPreferences("login_data", MODE_PRIVATE)
        mApiInterface = ApiClient.client!!.create(ApiInterface::class.java)
        Handler().postDelayed({
            if (!checkInternetConnection(this@SplashScreenActivity)) {
                val alertDialog = AlertDialog.Builder(this@SplashScreenActivity).create()
                alertDialog.setTitle("Tidak ada koneksi internet")
                alertDialog.setMessage("Cek koneksi internet anda dan coba lagi!")
                alertDialog.setButton(
                    DialogInterface.BUTTON_POSITIVE, "Coba Lagi"
                ) { dialog, which ->
                    finish()
                    startActivity(intent)
                }
                alertDialog.setCancelable(false)
                alertDialog.show()
            } else {
                if (!userLoggedIn.contains("token")) {
                    val intent = Intent(this@SplashScreenActivity, LoginActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                } else {
                    val username = userLoggedIn.getString("email",null)
                    val password = userLoggedIn.getString("password",null)

                    val deviceNumber = Settings.Secure.getString(
                        contentResolver,
                        Settings.Secure.ANDROID_ID
                    )

                    mApiInterface.login(username!!, password!!,deviceNumber).enqueue(object : Callback<Any> {
                        override fun onResponse(
                            call: Call<Any>,
                            response: Response<Any>
                        ) {
                            Log.d(packageName,response.raw().toString())

                            if (response.code() != 200) {
                                var jsonObject: JSONObject? = null
                                try {
                                    jsonObject = JSONObject(response.errorBody().string())
                                    val message: String = jsonObject.getString("message")
                                    Toast.makeText(applicationContext,message, Toast.LENGTH_LONG).show()

                                    Log.d(packageName,message)

                                    val intent = Intent(this@SplashScreenActivity, LoginActivity::class.java)
                                    startActivity(intent)
                                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                                    finish()
                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                }
                            } else {

                                var res = Gson().toJsonTree(response.body()).asJsonObject
                                Log.d(packageName,res.toString())
                                val editor = userLoggedIn.edit()
                                val data = res.get("data").asJsonObject
                                val userData = data.get("user").asJsonObject
                                val employeeData = userData.get("employee").asJsonObject

                                editor.putString("id", userData.get("id").asString)
                                editor.putString("name", employeeData.get("name").asString)
                                editor.putString("workunit_id", employeeData.get("workunit_id").asString)
                                editor.putString("email", userData.get("email").asString)
                                editor.putString("password", password)
                                editor.putString("role", userData.get("role").asString)
                                editor.putString("radius", userData.get("radius")?.asString)
                                editor.putString("employee_id", userData.get("employee_id").asInt.toString())
                                editor.putString("token", data.get("token").asString)

                                editor.putString("places",userData.getAsJsonArray("places").toString())

                                editor.apply()

                                val intent = Intent(this@SplashScreenActivity, MainActivity::class.java)
                                startActivity(intent)
                                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
                                finish()
                            }
                        }

                        override fun onFailure(call: Call<Any>, t: Throwable) {
                            Log.d(packageName, t.toString())
                            Toast.makeText(applicationContext,"Ada Kesalahan Server", Toast.LENGTH_LONG).show()
                        }
                    })
                }
            }
        }, 1000)
    }
}