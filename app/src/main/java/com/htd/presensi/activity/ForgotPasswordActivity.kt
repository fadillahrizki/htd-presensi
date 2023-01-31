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
import com.htd.presensi.databinding.ActivityForgotPasswordBinding
import com.htd.presensi.databinding.ActivityLoginBinding
import com.htd.presensi.rest.ApiClient
import com.htd.presensi.rest.ApiInterface
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.security.AccessController.getContext


class ForgotPasswordActivity : AppCompatActivity(), View.OnClickListener {
    lateinit var binding: ActivityForgotPasswordBinding
    lateinit var mApiInterface: ApiInterface
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())
        mApiInterface = ApiClient.client!!.create(ApiInterface::class.java)
        binding.btnSubmit.setOnClickListener(this)
        binding.btnLogin.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnSubmit.id -> submit()
            binding.btnLogin.id -> {
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }

    private fun submit() {
        var allTrue = true
        if (binding.email.editText?.text.toString().isEmpty()) {
            binding.email.error = "Email tidak boleh kosong"
            allTrue = false
        }
        if (allTrue) {
            binding.btnSubmit.setText("Loading...")
            val email: String = binding.email.editText?.text.toString()

            mApiInterface.forgotPassword(email).enqueue(object : Callback<Any> {
                override fun onResponse(
                    call: Call<Any>,
                    response: Response<Any>
                ) {
                    Log.d(packageName,response.raw().toString())

                    if (response.code() != 200) {
                        binding.cardError.setVisibility(View.VISIBLE)
                        binding.tvError.setText("Email Tidak Terdaftar!")
                        binding.cardSuccess.setVisibility(View.GONE)

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

                        binding.cardSuccess.setVisibility(View.VISIBLE)
                        binding.tvSuccess.setText("Email Berhasil dikirim, Silahkan Periksa Email Anda!")

                        binding.cardError.setVisibility(View.GONE)
                        binding.email.editText?.setText("")
                    }
                    binding?.btnSubmit.setText("Submit")
                }

                override fun onFailure(call: Call<Any>, t: Throwable) {
                    Log.d(packageName, t.toString())
                    Toast.makeText(applicationContext,"Gagal terhubung ke jaringan. Coba lagi",Toast.LENGTH_LONG).show()
                    binding.btnSubmit.setText("Submit")
                }
            })
        }
    }
}
