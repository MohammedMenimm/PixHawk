package com.example.pixhawk.utils

fun parseCoordsFromLine(line: String): List<Double> {
    val coordinatesPattern = Regex("-?\\d+\\.\\d+")
    val matches = coordinatesPattern.findAll(line)
    return matches.map { it.value.toDouble() }.toList()
}