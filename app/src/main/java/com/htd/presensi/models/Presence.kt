package com.htd.presensi.models

data class Presence(
    var id:String? = null,
    var attachment_url:String? = null,
    var pic_url:String? = null,
    var type:String? = null,
    var status:String? = null,
    var date:String? = null,
    var in_location:Boolean? = null,
    var lat:String? = null,
    var lng:String? = null,
    var time:String? = null,
    var started_at:String? = null,
    var finished_at:String? = null,
    var worktimeItem:String? = null,
    var time_left:String? = null,
    var persentase:String? = null,
)
