package com.example.pixhawk.utils

import kotlin.math.abs
import kotlin.math.floor

fun decdeg2dms(dd: Double): Triple<Int, Int, Double> {
    val isPositive = dd >= 0
    val absDd = abs(dd)
    val minutesAndSeconds = absDd * 3600
    val minutes = floor(minutesAndSeconds / 60).toInt()
    val seconds = minutesAndSeconds % 60
    val degrees = floor(minutes / 60.0).toInt()
    val remainingMinutes = minutes % 60
    return Triple(if (isPositive) degrees else -degrees, remainingMinutes, seconds)
}