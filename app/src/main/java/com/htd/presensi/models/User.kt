package com.htd.presensi.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    var id:String? = null,
    var name:String? = null,
    var email:String? = null,
    var role:String? = null,
    var radius:String? = null,
    var employeeId:String? = null,
    var token:String? = null,
) : Parcelable
