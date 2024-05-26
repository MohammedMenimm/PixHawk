package com.example.pixhawk.utils

fun decdeg2nmea(dd: Double): String {
    val (degrees, minutes, seconds) = decdeg2dms(dd)
    val decmin = seconds / 60
    return nodec(degrees).padStart(2, '0') + nodec(minutes).padStart(2, '0') + String.format("%.4f", decmin).substring(1).replace(',', '.')
}
