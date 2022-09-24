package com.htd.presensi.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.htd.presensi.models.Presence
import com.htd.presensi.models.Profile
import com.htd.presensi.models.Report
import com.htd.presensi.models.WorktimeItem

class MainViewModel : ViewModel() {
    var histories : MutableLiveData<ArrayList<Presence>> = MutableLiveData()
    var reports : MutableLiveData<Report> = MutableLiveData()
    var profile : MutableLiveData<Profile> = MutableLiveData()
    var historyDetail : MutableLiveData<Presence> = MutableLiveData()
    var worktimeItems : MutableLiveData<ArrayList<WorktimeItem>> = MutableLiveData()
    var activeWorktime : MutableLiveData<WorktimeItem> = MutableLiveData()
    var times : MutableLiveData<String> = MutableLiveData()
    var paidLeaves : MutableLiveData<ArrayList<String>> = MutableLiveData()
}