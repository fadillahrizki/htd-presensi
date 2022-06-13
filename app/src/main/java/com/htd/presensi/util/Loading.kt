package com.htd.presensi.util

import android.app.Dialog
import android.content.Context
import com.htd.presensi.R

class Loading(context: Context) {
    var dialog = Dialog(context)

    init {
     dialog.setContentView(R.layout.loading)
    }

    fun show(){
        dialog.show()
    }

    fun hide(){
        dialog.hide()
    }

    fun dismiss(){
        dialog.dismiss()
    }
}