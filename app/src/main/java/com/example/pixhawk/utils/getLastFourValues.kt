package com.example.pixhawk.utils

fun getLastFourValues(line: String): List<String> {
    return line.trim().split(",").takeLast(4)
}