package com.htd.presensi.models

data class WorktimeItem(
    var id:String? = null,
    var worktime_id:String? = null,
    var name:String? = null,
    var start_time:String? = null,
    var end_time:String? = null,
    var on_time_start:String? = null,
    var on_time_end:String? = null,
)
