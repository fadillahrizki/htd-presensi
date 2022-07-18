package com.htd.presensi.models

data class Presence(
    var id:String? = null,
    var attachment_url:String? = null,
    var pic_url:String? = null,
    var type:String? = null,
    var status:String? = null,
    var date:String? = null,
    var in_location:String? = null,
    var lat:String? = null,
    var lng:String? = null,
    var time:String? = null,
    var worktimeItem:String? = null,
)
