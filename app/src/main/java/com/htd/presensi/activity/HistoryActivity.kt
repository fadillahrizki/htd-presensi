package com.htd.presensi.activity

import android.R
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.htd.presensi.adapter.HistoryAdapter
import com.htd.presensi.databinding.ActivityHistoryBinding
import com.htd.presensi.models.Presence
import com.htd.presensi.models.Report
import com.htd.presensi.rest.ApiClient
import com.htd.presensi.rest.ApiInterface
import com.htd.presensi.util.CustomDatePickerDialog
import com.htd.presensi.util.Loading
import com.htd.presensi.viewmodel.MainViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity(), View.OnClickListener,
    AdapterView.OnItemSelectedListener {
    lateinit var binding: ActivityHistoryBinding
    lateinit var userLoggedIn: SharedPreferences
    lateinit var mApiInterface: ApiInterface
    lateinit var jenisAbsensiAdapter: ArrayAdapter<String>
    lateinit var statusAdapter: ArrayAdapter<String>

    lateinit var loading:Loading

    lateinit var historyAdapter: HistoryAdapter
    var mainViewModel: MainViewModel = MainViewModel()

    var jenisAbsensi = arrayOf("Semua","Hadir","Izin","Sakit")
    var status = arrayOf("Semua","Diterima","Ditolak","Diajukan")

    var selectedJenisAbsensi = "Semua"
    var selectedStatus = "Semua"

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
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        historyAdapter = HistoryAdapter(this)
        mApiInterface = ApiClient.client!!.create(ApiInterface::class.java)

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

        binding.filter.fromDate.text = CustomDatePickerDialog.year.toString()+"-"+(CustomDatePickerDialog.month+1).toString()+"-01"
        binding.filter.toDate.text = CustomDatePickerDialog.year.toString()+"-"+(CustomDatePickerDialog.month+1).toString()+"-"+CustomDatePickerDialog.day.toString()

        binding.rvPresences.layoutManager = LinearLayoutManager(this)
        binding.rvPresences.adapter = historyAdapter

        loading = Loading(this)

    }

    fun observe(){
        mainViewModel.histories.observe(this) { data ->
            historyAdapter.data = data
            historyAdapter.notifyDataSetChanged()
            loading.hide()
        }

        mainViewModel.reports.observe(this) { data ->
            binding.hadir.text = data.hadir
            binding.alfa.text = data.alfa
            binding.cuti.text = data.cuti
            binding.hariKerja.text = data.hari_kerja
            binding.waktuTelat.text = data.waktu_telat
            binding.persentase.text = data.persentase
        }
    }

    fun api(){
        getData(binding.filter.fromDate.text.toString(),binding.filter.toDate.text.toString())
    }

    fun listener(){
        binding.filter.fromDate.setOnClickListener(this)
        binding.filter.toDate.setOnClickListener(this)
        binding.filter.filterBtn.setOnClickListener(this)
        binding.filter.jenisAbsensi.setOnItemSelectedListener(this)
        binding.filter.status.setOnItemSelectedListener(this)
    }

    fun getData(start: String, end: String) {
        loading.show()
        mApiInterface.getPresences(userLoggedIn.getString("token",null)!!,userLoggedIn.getString("employee_id",null)!!,if(selectedJenisAbsensi == "Semua") "" else selectedJenisAbsensi,if(selectedStatus == "Semua") "" else selectedStatus,start,end).enqueue(object :
            Callback<Any> {
            override fun onResponse(
                call: Call<Any>,
                response: Response<Any>
            ) {
                var res = Gson().toJsonTree(response.body()).asJsonObject
                var data = res.getAsJsonObject("data")

                Log.d("histories",data.toString())


                val presences = data.getAsJsonArray("presences")

                var arrPresences = ArrayList<Presence>()
                val sdf = SimpleDateFormat("yyyy-MM-dd")

                for(p in presences){
                    val obj = p.asJsonObject
                    val date = sdf.parse(obj.get("date").asString)

                    var presence = Presence()
                    presence.id = obj.get("id").asString
                    presence.attachment_url = obj.get("attachment_url")?.asString
                    presence.pic_url = obj.get("pic_url")?.asString
                    presence.lat = obj.get("lat")?.asString
                    presence.lng = obj.get("lng")?.asString
                    presence.type = obj.get("type").asString
                    presence.in_location = obj.get("in_location").asBoolean
                    presence.status = obj.get("status").asString
                    presence.worktimeItem = if(obj.get("worktime_item") != null) obj.get("worktime_item").asJsonObject.get("name").asString else ""
                    presence.date = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id")).format(date)
                    presence.time = obj.get("time")?.asString
                    presence.persentase = obj.get("presentase")?.asString

                    if(obj.get("time_left") != null){

                        var time_left_int = obj.get("time_left").asInt
                        var time_left = ""

                        // terlalu cepat
                        if(presence.worktimeItem == "Masuk")
                        {
                            time_left = "Keterlambatan "+time_left_int+" Menit ("+presence.persentase+"%)"
                        }else if(presence.worktimeItem == "Pulang")
                        {
                            time_left = "Sebelum Waktu "+time_left_int+" Menit ("+presence.persentase+"%)"
                        }else{
                            time_left = "Tepat Waktu"
                        }

                        presence.time_left = time_left
                    }

                    arrPresences.add(presence)
                }

                mainViewModel.histories.postValue(arrPresences)

            }

            override fun onFailure(call: Call<Any>, t: Throwable) {
                Log.d(packageName, t.toString())
            }
        })

        mApiInterface.reports(userLoggedIn.getString("token",null)!!,userLoggedIn.getString("workunit_id",null)!!,start,end,userLoggedIn.getString("name",null)!!).enqueue(object : Callback<Any> {
            override fun onResponse(
                call: Call<Any>,
                response: Response<Any>
            ) {
                var res = Gson().toJsonTree(response.body()).asJsonObject
                var dt = res.getAsJsonObject("data")
                var data = dt.getAsJsonArray("data")
                var report = Report()
                if(data.size() > 0){
                    report.hadir = data.get(0).asJsonObject.get("hadir").asInt.toString()
                    report.alfa = data.get(0).asJsonObject.get("alfa").asInt.toString()
                    report.cuti = data.get(0).asJsonObject.get("cuti").asInt.toString()
                    report.hari_kerja = data.get(0).asJsonObject.get("hari_kerja").asInt.toString()
                    report.waktu_telat = data.get(0).asJsonObject.get("time_left").asInt.toString()
                    report.persentase = data.get(0).asJsonObject.get("presentase").asString
                }else{
                    report.hadir = "0"
                    report.alfa = "0"
                    report.cuti = "0"
                    report.hari_kerja = "0"
                    report.waktu_telat = "0"
                    report.persentase = "0%"
                }
                mainViewModel.reports.postValue(report)
                Log.d("reports",data.toString())
            }

            override fun onFailure(call: Call<Any>, t: Throwable) {
                Log.d(packageName, t.toString())
            }
        })
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
        when(p0?.id){
            binding.filter.jenisAbsensi.id -> {
                selectedJenisAbsensi = jenisAbsensi[idx]
            }
            binding.filter.status.id -> selectedStatus = status[idx]
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

}