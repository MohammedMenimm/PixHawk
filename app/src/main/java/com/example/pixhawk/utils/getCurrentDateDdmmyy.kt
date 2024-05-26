package com.example.pixhawk.utils

import java.text.SimpleDateFormat
import java.util.Date

fun getCurrentDateDdmmyy(): String {
    val currentDate = System.currentTimeMillis()
    val formattedDate = SimpleDateFormat("ddMMyy").format(Date(currentDate))
    return formattedDate
}