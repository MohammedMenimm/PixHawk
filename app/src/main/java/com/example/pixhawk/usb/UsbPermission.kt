package com.example.pixhawk.usb

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.example.pixhawk.viewModel.LogViewModel
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import java.io.IOException
class UsbPermission(private val logViewModel: LogViewModel) {
    @Composable
    fun RequestUsbPermission(context: Context, driver: UsbSerialDriver?, usbSerialPort: MutableState<UsbSerialPort?>,
                             connectedToUsb: MutableState<Boolean>,alreadyGivenPermission: MutableState<Boolean>) {

        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val usbDevice = driver?.device

        if (usbDevice == null) {
            logViewModel.addLog("No USB device found.")
            return
        }

        if (usbManager.hasPermission(usbDevice)) {
            // Permission already granted
            connectToUsbDevice(usbManager, usbDevice, driver, usbSerialPort,connectedToUsb)
        } else {
            // Request permission
            val permissionIntent = PendingIntent.getBroadcast(context, 0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            val receiver = UsbPermissionReceiver(
                onPermissionGranted = { device ->
                    alreadyGivenPermission.value = true
                    connectToUsbDevice(usbManager, device, driver, usbSerialPort,connectedToUsb)
                },
                onPermissionDenied = {
                    logViewModel.addLog("Permission denied for USB device")
                    Log.i("Mohammed", "Permission denied for USB device")
                })

            val filter = IntentFilter(ACTION_USB_PERMISSION)
            context.registerReceiver(receiver, filter)

            usbManager.requestPermission(usbDevice, permissionIntent)
        }
    }
    private fun connectToUsbDevice(
        usbManager: UsbManager,
        usbDevice: UsbDevice,
        driver: UsbSerialDriver?,
        usbSerialPort: MutableState<UsbSerialPort?>,
        connectedToUsb: MutableState<Boolean>
    ) {
        val port = driver?.ports?.get(0)
        val connection = usbManager.openDevice(usbDevice)
        try {
            port?.apply {
                if (!isOpen) {
                    open(connection)
                    setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
                    usbSerialPort.value = this
                    connectedToUsb.value = true
                    logViewModel.addLog("USB device connected")
                    Log.i("Mohammed", "USB device connected")
                } else {
                    logViewModel.addLog("Port is already open")
                    Log.i("Mohammed", "Port is already open")
                    connectedToUsb.value = true
                }
            }
        } catch (e: IOException) {
            logViewModel.addLog("Error opening port: ${e.message}")
            connectedToUsb.value = false
            usbSerialPort.value = null
        }
    }
    companion object {
        const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    }
}
