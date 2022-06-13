package com.htd.presensi.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.htd.presensi.models.Presence
import com.htd.presensi.models.Profile

class MainViewModel : ViewModel() {
    var histories : MutableLiveData<ArrayList<Presence>> = MutableLiveData()
    var profile : MutableLiveData<Profile> = MutableLiveData()
}