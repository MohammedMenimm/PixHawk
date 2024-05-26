package com.example.pixhawk.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun getNmeaTime(): String {
    val currentTime = Date()
    val dateFormat = SimpleDateFormat("HHmmss.SSS", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return dateFormat.format(currentTime)
}