package com.htd.presensi.util

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.widget.TextView
import java.util.*

object CustomDatePickerDialog {
    var calendar = Calendar.getInstance()
    var year = calendar[Calendar.YEAR]
    var month = calendar[Calendar.MONTH]
    var day = calendar[Calendar.DAY_OF_MONTH]
    fun show(activity: Activity, tv: TextView) {
        val txt = tv.text.toString().split("-").toTypedArray()
        val dialog = DatePickerDialog(
            activity,
            { view, year, month, dayOfMonth ->
                var month = month
                month = month + 1
                val date = "$year-$month-$dayOfMonth"
                tv.text = date
            }, txt[0].toInt(), txt[1].toInt() - 1, txt[2].toInt()
        )
        dialog.show()
    }
}