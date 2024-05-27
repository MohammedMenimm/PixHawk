package com.example.pixhawk.utils

import android.util.Log
import androidx.compose.runtime.MutableState
import com.hoho.android.usbserial.driver.UsbSerialPort
import java.io.IOException

fun sendNmea(payload: String, port: UsbSerialPort?) {
    val sentence = "\$" + payload + "*" + calculateNMEAChecksum(payload) + "\r\n"
    try {
        port?.write(sentence.toByteArray(Charsets.US_ASCII),sentence.length)
        Log.i("Sending: ", sentence)

    } catch (e: IOException) {
            return
    }
}