package com.example.pixhawk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.pixhawk.dialogs.UsbDriverDialog
import com.example.pixhawk.screen.LogScreen
import com.example.pixhawk.screen.PixHawkHomeScreen
import com.example.pixhawk.usb.UsbPermission.Companion.ACTION_USB_PERMISSION
import com.example.pixhawk.viewModel.LogViewModel
import com.hoho.android.usbserial.driver.UsbSerialPort
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.floor

class PixHawk(private val logViewModel: LogViewModel){
    @Composable
    fun PixHawkApp() {
        val context = LocalContext.current
        rememberCoroutineScope()
        val usbSerialPort = remember { mutableStateOf<UsbSerialPort?>(null) }
        val connectedToUsb = remember { mutableStateOf(false) }
        val transmittingData = remember { mutableStateOf(false) }
        val alreadyGivenPermission =  remember { mutableStateOf(false) }

        val exampleFileContent2 = """
            DOP: 0.3,0.3,0.3,21
            Pos: 55.882007,13.001804,26.8,21,0.3
            Pos: 55.882025,13.001751,27.3,21,0.3
        """.trimIndent()

        DisposableEffect(context) {
            val usbDetachedReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        if (device != null) {
                            Log.d("USB", "Device detached: $device")
                            connectedToUsb.value = false
                            usbSerialPort.value = null
                        }
                    }
                }
            }

            val filter = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)
            context.registerReceiver(usbDetachedReceiver, filter)

            onDispose {
                context.unregisterReceiver(usbDetachedReceiver)
            }
        }

        Column {
            UsbDriverDialog(context = context,usbSerialPort,connectedToUsb,alreadyGivenPermission, logViewModel)

           if(usbSerialPort.value != null) {
                transmittingData.value = true
                TransmitData(fileContent = exampleFileContent2, usbSerialPort = usbSerialPort, connectedToUsb,transmittingData)
            }

            PixHawkHomeScreen(
                connectedToUsb = connectedToUsb,
                transmittingData = transmittingData
            )
            LogScreen(logViewModel)
        }
    }
    @Composable
    fun TransmitData(fileContent: String, usbSerialPort: MutableState<UsbSerialPort?>,connectedToUsb: MutableState<Boolean>,transmittingData: MutableState<Boolean>) {
        var lines by remember { mutableStateOf(fileContent.split("\n")) }

        LaunchedEffect(Unit) {
            launch {
                try {

                while (true) {
                    lines.forEach { line ->
                        if (line.startsWith("Pos:")) {
                            val coords = parseCoordsFromLine(line)
                            val lat = decdeg2nmea(coords[0])
                            val long = decdeg2nmea(coords[1])
                            val alt = coords[2]
                            val currTime = getNmeaTime()
                            val quality = getLastTwoValues(line)
                            val sats = quality[0] // Sats ex. 07
                            val hdop = quality[1] // HDOP ex. 1.3

                            val gga = "GPGGA,$currTime,$lat,N,$long,E,1,$sats,$hdop,$alt,M,,,,0000"
                            val checksum = calculateNMEAChecksum(gga)
                            val ggaWithChecksum = "\$${gga}*${checksum}"
                            if(usbSerialPort == null) println("SendingFake: $ggaWithChecksum") else{
                                sendNmea(gga, usbSerialPort.value,connectedToUsb)
                            }

                            val currDate = getCurrentDateDdmmyy()
                            val rmc = "GPRMC,$currTime,A,$lat,N,$long,E,000.5,054.7,$currDate,020.3,E"
                            val rmcChecksum = calculateNMEAChecksum(rmc)
                            val rmcWithChecksum = "\$${rmc}*${rmcChecksum}"
                            if(usbSerialPort == null ) println("SendingFake: $rmcWithChecksum") else {
                                sendNmea(rmc, usbSerialPort.value,connectedToUsb)
                            }
                        }

                        if (line.startsWith("DOP:")) {
                            val dops = parseCoordsFromLine(line)
                            val pdop = dops[0]
                            val hdop = dops[1]
                            val vdop = dops[2]
                            val gsa = "GPGSA,A,10,11,12,13,14,15,16,17,18,19,20,21,22,$pdop,$hdop,$vdop"
                            val checksum = calculateNMEAChecksum(gsa)
                            val gsaWithChecksum = "\$${gsa}*${checksum}"
                            if(usbSerialPort == null ) println("SendingFake: $gsaWithChecksum") else {
                                sendNmea(gsa, usbSerialPort.value, connectedToUsb)
                            }
                        }
                        delay(1000)// Delay for 0.1 seconds
                    }
                    // Reset the lines
                    lines = fileContent.split("\n")
                }
            } catch (e: Exception) {
                    transmittingData.value = false
                    return@launch
                    Log.e("TransmitData", "Error in data transmission loop", e)
                }
            }
        }
    }
    fun parseCoordsFromLine(line: String): List<Double> {
        val coordinatesPattern = Regex("-?\\d+\\.\\d+")
        val matches = coordinatesPattern.findAll(line)
        return matches.map { it.value.toDouble() }.toList()
    }

    fun getLastTwoValues(line: String): List<String> {
        return line.trim().split(",").takeLast(2)
    }

    fun getNmeaTime(): String {
        val currentTime = Date()
        val dateFormat = SimpleDateFormat("HHmmss.SSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return dateFormat.format(currentTime)
    }

    fun getCurrentDateDdmmyy(): String {
        val currentDate = System.currentTimeMillis()
        val formattedDate = SimpleDateFormat("ddMMyy").format(Date(currentDate))
        return formattedDate
    }

    fun decdeg2nmea(dd: Double): String {
        val (degrees, minutes, seconds) = decdeg2dms(dd)
        val decmin = seconds / 60
        return nodec(degrees).padStart(2, '0') + nodec(minutes).padStart(2, '0') + String.format("%.4f", decmin).substring(1).replace(',', '.')
    }

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

    private fun nodec(dec: Int): String {
        return dec.toString()
    }

    fun calculateNMEAChecksum(sentence: String): String {
        var checksum = 0
        for (c in sentence.toCharArray()) {
            checksum = checksum xor c.toInt()
        }
        return checksum.toString(16).padStart(2, '0').uppercase()
    }

    private fun sendNmea(payload: String, port: UsbSerialPort?,connectedToUsb: MutableState<Boolean> ) {
        val sentence = "\$" + payload + "*" + calculateNMEAChecksum(payload) + "\r\n"
        try {
            port?.write(sentence.toByteArray(Charsets.US_ASCII),sentence.length)
            logViewModel.addLog("Sending: $sentence")

        } catch (e: IOException) {
            if(port == null){
                connectedToUsb.value = false
                return
            }
            e.printStackTrace()
        }
    }

    val usbPermissionActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val usbDevice: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        usbDevice?.apply {
                        }
                    } else {
                        Log.d("USB", "permission denied for device $usbDevice")
                    }
                }
            }
        }
    }
}

