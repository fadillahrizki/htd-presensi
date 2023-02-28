package com.htd.presensi.activity

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.htd.presensi.R
import com.htd.presensi.adapter.LogAdapter
import com.htd.presensi.databinding.ActivityLogBinding
import com.htd.presensi.helper.DBHelper
import com.htd.presensi.models.Log
import com.htd.presensi.util.Loading
import com.htd.presensi.viewmodel.MainViewModel
import java.util.*

class LogActivity : AppCompatActivity() {
    lateinit var binding: ActivityLogBinding
    private lateinit var userLoggedIn: SharedPreferences
    lateinit var dbHelper: DBHelper
    lateinit var logAdapter: LogAdapter

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
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        userLoggedIn = getSharedPreferences("login_data", MODE_PRIVATE)
        dbHelper = DBHelper(this, null)
        logAdapter = LogAdapter(this)

        supportActionBar?.title = "Log Ativitas"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.rvLogs.layoutManager = LinearLayoutManager(this)
        binding.rvLogs.adapter = logAdapter

        loading = Loading(this)
    }

    fun observe(){
        mainViewModel.logs.observe(this) { data ->
            logAdapter.data = data
            logAdapter.notifyDataSetChanged()
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

        var arrLogs = ArrayList<Log>()

        val cursor = dbHelper.getLogs()
        
        with(cursor){
            while(moveToNext()){
                var log = Log()
                log.id = getString(getColumnIndexOrThrow(DBHelper.ID_COL))
                log.date = getString(getColumnIndexOrThrow(DBHelper.DATE_COL))
                log.text = getString(getColumnIndexOrThrow(DBHelper.TEXT_COL))
                arrLogs.add(log)
            }
        }

        // at last we close our cursor
        cursor!!.close()

        mainViewModel.logs.postValue(arrLogs)
    }
}