package com.example.pixhawk

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.floor

class PixHawk{
    @Composable
    fun PixHawkApp() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var usbSerialPort by remember { mutableStateOf<UsbSerialPort?>(null) }

        val exampleFileContent2 = """
            DOP: 0.3,0.3,0.3,21
            Pos: 55.882007,13.001804,26.8,21,0.3
            Pos: 55.882025,13.001751,27.3,21,0.3
        """.trimIndent()

        LaunchedEffect(Unit) {
            scope.launch(Dispatchers.IO) {
                try {
                    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                    val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
                    if (availableDrivers.isEmpty()) {
                        Log.i("Mohammed","No USB devices found.")

                        return@launch
                    }

                    val driver = availableDrivers[0]
                    val usbDevice = driver.device

                    // Request permission
                    val permissionIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        Intent(ACTION_USB_PERMISSION),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    usbManager.requestPermission(usbDevice, permissionIntent)

                    val port = driver.ports[0]
                    Log.i("Mohammed","${driver.ports}")

                    val connection = usbManager.openDevice(driver.device) ?: run {
                        Log.i("Mohammed","Could not open connection to USB device")

                        return@launch
                    }

                    port.open(connection)
                    port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
                    usbSerialPort = port
                    Log.i("Mohammed","USB device connected.")
                    Log.i("mohammed","$port")

                } catch (e: IOException) {
                    Log.i("mohammed","failed to connect to any USB device, ERROR :$e")
                }
            }
        }
        if(usbSerialPort != null) sendNMEA(fileContent = exampleFileContent2, usbSerialPort = usbSerialPort)
    }

    @Composable
    fun sendNMEA(fileContent: String, usbSerialPort: UsbSerialPort?) {
        var lines by remember { mutableStateOf(fileContent.split("\n")) }

        LaunchedEffect(Unit) {
            launch {
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
                                sendNmea(gga, usbSerialPort)
                            }


                            val currDate = getCurrentDateDdmmyy()
                            val rmc = "GPRMC,$currTime,A,$lat,N,$long,E,000.5,054.7,$currDate,020.3,E"
                            val rmcChecksum = calculateNMEAChecksum(rmc)
                            val rmcWithChecksum = "\$${rmc}*${rmcChecksum}"
                            if(usbSerialPort == null ) println("SendingFake: $rmcWithChecksum") else {
                                sendNmea(rmc, usbSerialPort)
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
                                sendNmea(gsa, usbSerialPort)
                            }
                        }

                        delay(100) // Delay for 0.1 seconds

                        val buffer = ByteArray(1024)
                        val timeout = 1000 // 1 second timeout for reading

                        // Read data from the serial port
                        val bytesRead = usbSerialPort?.read(buffer, timeout)

                        if (bytesRead != null) {
                            if (bytesRead > 0) {
                                // Convert the read bytes to a string
                                val response = String(buffer, 0, bytesRead, Charsets.US_ASCII)
                                println("MohammedRead: $response")
                            } else {
                                println("MohammedRead Failed,  response received within the timeout period.")
                            }
                        }

                    }
                    // Reset the lines
                    lines = fileContent.split("\n")
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

    /*may use later on

    fun waitNextUpdate(updateFrequency: Int) {
        val sleepTime = (1.0 / updateFrequency * 1000).toLong() // Convert frequency to milliseconds
        Thread.sleep(sleepTime)
    } */

    fun decdeg2nmea(dd: Double): String {
        /**
         * Calculate the NMEA degree format for the given angle in degrees.
         *
         * @param dd The angle in degrees
         * @return The angle as a DDMM.mmmm string
         */
        val (degrees, minutes, seconds) = decdeg2dms(dd)
        val decmin = seconds / 60
        return nodec(degrees).padStart(2, '0') + nodec(minutes).padStart(2, '0') + String.format("%.4f", decmin).substring(1).replace(',', '.')
    }

    fun decdeg2dms(dd: Double): Triple<Int, Int, Double> {
        /**
         * Convert decimal degrees to degrees, minutes, and seconds.
         *
         * @param dd The angle in decimal degrees
         * @return A Triple containing degrees, minutes, and seconds
         */
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
        /**
         * Removes decimals.
         * @return The number as a string with no decimals
         */
        return dec.toString()
    }

    fun calculateNMEAChecksum(sentence: String): String {
        var checksum = 0
        for (c in sentence.toCharArray()) {
            checksum = checksum xor c.toInt()
        }
        return checksum.toString(16).padStart(2, '0').uppercase()
    }

    private fun sendNmea(payload: String, port: UsbSerialPort) {
        val sentence = "\$" + payload + "*" + calculateNMEAChecksum(payload) + "\r\n"

        try {
            port.write(sentence.toByteArray(Charsets.US_ASCII),sentence.length)
            println("Sent NMEA To Port: $sentence")
            // Buffer to store incoming data

        } catch (e: IOException) {
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
                            // Permission granted, perform your operations here
                        }
                    } else {
                        Log.d("USB", "permission denied for device $usbDevice")
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    }
}

