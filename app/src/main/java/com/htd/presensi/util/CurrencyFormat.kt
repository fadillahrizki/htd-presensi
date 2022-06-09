package com.htd.presensi.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

object CurrencyFormat {

    fun format(value:Double):String{
        val formatter = NumberFormat.getInstance(Locale.US) as DecimalFormat
        formatter.applyPattern("#,###,###,###")
        val formattedString: String = formatter.format(value)
        return "Rp. "+formattedString
    }
}