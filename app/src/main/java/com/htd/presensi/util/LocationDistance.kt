package com.htd.presensi.util

import android.location.Location

object LocationDistance {
    fun betweenCoordinates(loc1 : Location,loc2 : Location) : Double{
        var earthRadius = 6371;

        var dLat = this.degreesToRadians(loc2.latitude-loc1.latitude)
        var dLon = this.degreesToRadians(loc2.longitude-loc1.longitude)

        var a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(loc1.latitude) * Math.cos(loc2.latitude)
        var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))

        return earthRadius * 1000 * c
    }

    fun degreesToRadians(degrees : Double): Double {
        return degrees * Math.PI / 180
    }
}