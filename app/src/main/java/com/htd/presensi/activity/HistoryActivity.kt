package com.htd.presensi.activity

import android.R
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.htd.presensi.databinding.ActivityHistoryBinding
import com.htd.presensi.util.CustomDatePickerDialog

class HistoryActivity : AppCompatActivity(), View.OnClickListener,
    AdapterView.OnItemSelectedListener {
    lateinit var binding: ActivityHistoryBinding
    lateinit var userLoggedIn: SharedPreferences
    lateinit var jenisAbsensiAdapter: ArrayAdapter<String>
    lateinit var statusAdapter: ArrayAdapter<String>

    var jenisAbsensi = arrayOf("Hadir","Izin","Sakit")
    var status = arrayOf("Diterima","Ditolak")

    var selectedJenisAbsensi = "Hadir"
    var selectedStatus = "Diterima"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        supportActionBar?.title = "Riwayat Absensi"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        userLoggedIn = getSharedPreferences("login_data", MODE_PRIVATE)

        jenisAbsensiAdapter = ArrayAdapter(this, R.layout.simple_spinner_item, jenisAbsensi)
        jenisAbsensiAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.filter.jenisAbsensi.adapter = jenisAbsensiAdapter

        statusAdapter = ArrayAdapter(this, R.layout.simple_spinner_item, status)
        statusAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.filter.status.adapter = statusAdapter

        binding.filter.fromDate.setOnClickListener(this)
        binding.filter.toDate.setOnClickListener(this)
        binding.filter.filterBtn.setOnClickListener(this)
        binding.filter.jenisAbsensi.setOnItemSelectedListener(this)
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
            binding.filter.fromDate.id -> CustomDatePickerDialog.show(this, binding.filter.fromDate)
            binding.filter.toDate.id -> CustomDatePickerDialog.show(this, binding.filter.toDate)
            binding.filter.filterBtn.id -> getData(
                binding.filter.fromDate.text.toString(),
                binding.filter.toDate.text.toString()
            )
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, view: View?, idx: Int, p3: Long) {
        when(view?.id){
            binding.filter.jenisAbsensi.id -> selectedJenisAbsensi = jenisAbsensi[idx]
            binding.filter.status.id -> selectedStatus = status[idx]
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

}