package com.example.pixhawk.utils

fun getLastTwoValues(line: String): List<String> {
    return line.trim().split(",").takeLast(2)
}