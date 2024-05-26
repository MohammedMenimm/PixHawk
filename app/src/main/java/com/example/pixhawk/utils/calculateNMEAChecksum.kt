package com.example.pixhawk.utils

fun calculateNMEAChecksum(sentence: String): String {
    var checksum = 0
    for (c in sentence.toCharArray()) {
        checksum = checksum xor c.toInt()
    }
    return checksum.toString(16).padStart(2, '0').uppercase()
}