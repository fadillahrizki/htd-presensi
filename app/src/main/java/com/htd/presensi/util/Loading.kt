package com.htd.presensi.util

import android.app.Dialog
import android.content.Context
import android.view.ViewGroup
import com.htd.presensi.R

class Loading(context: Context) {
    var dialog = Dialog(context)

    init {
        dialog.setContentView(R.layout.loading)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(false)
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