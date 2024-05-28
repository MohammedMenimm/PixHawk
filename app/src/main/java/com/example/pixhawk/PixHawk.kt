package com.example.pixhawk

import Gps
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
import com.example.pixhawk.usb.RequestUsbPermission
import com.example.pixhawk.utils.decdeg2nmea
import com.example.pixhawk.utils.getCurrentDateDdmmyy
import com.example.pixhawk.utils.getLastFourValues
import com.example.pixhawk.utils.getNmeaTime
import com.example.pixhawk.utils.parseCoordsFromLine
import com.example.pixhawk.utils.sendNmea
import com.example.pixhawk.viewModel.LogViewModel
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PixHawk(private val logViewModel: LogViewModel){
    @Composable
    fun PixHawkApp() {
        val context = LocalContext.current
        rememberCoroutineScope()
        val usbSerialPort = remember { mutableStateOf<UsbSerialPort?>(null) }
        val connectedToUsb = remember { mutableStateOf(false) }
        val transmittingData = remember { mutableStateOf(false) }
        val alreadyGivenPermission =  remember { mutableStateOf(false) }
        val stringOfGpsAndDops = remember { mutableStateOf(StringBuilder()) }
        val startSendingNmea =  remember { mutableStateOf(false) }
        val hasLocationEnabled =  remember { mutableStateOf(false) }
        val showDialog = remember { mutableStateOf(true) }

        Column {
            UsbDriverDialog(context = context,usbSerialPort,connectedToUsb,alreadyGivenPermission,showDialog,logViewModel)

            if(usbSerialPort.value != null && startSendingNmea.value) {
                transmittingData.value = true
                TransmitData(stringBuilder = stringOfGpsAndDops, usbSerialPort = usbSerialPort, transmittingData)
            }

            PixHawkHomeScreen(connectedToUsb = connectedToUsb, transmittingData = transmittingData,startSendingNmea)
            Gps(context,logViewModel,stringOfGpsAndDops,hasLocationEnabled).ShowGpsInformation()
            LogScreen(logViewModel)
        }

        DisposableEffect(context) {
            val usbDetachedReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        if (device != null) {
                            Log.d("USB", "Device detached: $device")
                            logViewModel.addLog("USB device detached")
                            connectedToUsb.value = false
                            startSendingNmea.value = false
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

        DisposableEffect(context) {
            val usbAttachedReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        if (device != null) {
                            Log.i("USB", "Device Attached: $device")
                            logViewModel.addLog("USB device attached")

                            val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
                            if (context != null) {
                                showDialog.value = false
                                RequestUsbPermission(logViewModel)
                                    .requestUsbPermission(context, driver, usbSerialPort, connectedToUsb,alreadyGivenPermission)
                            }
                        }
                    }
                }
            }

            val filter = IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            context.registerReceiver(usbAttachedReceiver, filter)

            onDispose {
                context.unregisterReceiver(usbAttachedReceiver)
            }
        }
    }
    @Composable
    fun TransmitData(stringBuilder: MutableState<StringBuilder>, usbSerialPort: MutableState<UsbSerialPort?>, transmittingData: MutableState<Boolean>) {
        var lines by remember { mutableStateOf(stringBuilder.value.toString().split("\n")) }

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
                                val quality = getLastFourValues(line)
                                val sats = quality[0] // Sats ex. 07
                                val hdop = quality[1] // HDOP ex. 1.3
                                val speed = quality[2]
                                val heading = quality[3]

                                val gga = "GPGGA,$currTime,$lat,N,$long,E,1,$sats,$hdop,$alt,M,,,,0000"
                                sendNmea(gga, usbSerialPort.value)

                                val currDate = getCurrentDateDdmmyy()
                                val rmc = "GPRMC,$currTime,A,$lat,N,$long,E,$speed,$heading,$currDate,020.3,E"
                                sendNmea(rmc, usbSerialPort.value)

                            }

                            if (line.startsWith("DOP:")) {
                                val dops = parseCoordsFromLine(line)
                                val pdop = dops[0]
                                val hdop = dops[1]
                                val vdop = dops[2]
                                val gsa = "GPGSA,A,10,11,12,13,14,15,16,17,18,19,20,21,22,$pdop,$hdop,$vdop"
                                sendNmea(gsa, usbSerialPort.value)

                            }
                            delay(100) // Delay for 0.1 seconds
                        }
                        // Reset the lines
                        lines = stringBuilder.value.split("\n")
                    }
                } catch (e: Exception) {
                    transmittingData.value = false
                    Log.e("TransmitData", "Stopped Transmission", e)
                    return@launch
                }
            }
        }
    }
}

