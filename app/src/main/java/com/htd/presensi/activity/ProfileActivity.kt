package com.htd.presensi.activity

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.htd.presensi.databinding.ActivityProfileBinding
import com.htd.presensi.util.CustomDatePickerDialog

class ProfileActivity : AppCompatActivity(), View.OnClickListener {
    lateinit var binding: ActivityProfileBinding
    private lateinit var userLoggedIn: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        userLoggedIn = getSharedPreferences("login_data", MODE_PRIVATE)

        supportActionBar?.title = "Profil"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    fun getData(start: String?, end: String?) {
        Toast.makeText(applicationContext,"Start : ${start}\nEnd : ${end}",Toast.LENGTH_LONG).show()
    }

    override fun onClick(view: View?) {
        when (view?.id) {

        }
    }

}