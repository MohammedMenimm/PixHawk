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
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import java.io.IOException
class UsbPermission {
    @Composable
    fun RequestUsbPermission(context: Context, driver: UsbSerialDriver?, usbSerialPort: MutableState<UsbSerialPort?>) {

        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val usbDevice = driver?.device

        if (usbDevice == null) {
            Log.e("Mohammed", "No USB device found.")
            return
        }

        if (usbManager.hasPermission(usbDevice)) {
            // Permission already granted
            connectToUsbDevice(usbManager, usbDevice, driver, usbSerialPort)
        } else {
            // Request permission
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            val receiver = UsbPermissionReceiver(
                onPermissionGranted = { device ->
                    connectToUsbDevice(usbManager, device, driver, usbSerialPort)
                },
                onPermissionDenied = {
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
        usbSerialPort: MutableState<UsbSerialPort?>
    ) {
        val port = driver?.ports?.get(0)

        val connection = usbManager.openDevice(usbDevice)
        try {
            port?.apply {
                if (!isOpen) {
                    open(connection)
                    setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
                    usbSerialPort.value = this
                    Log.i("Mohammed", "USB device connected.")
                } else {
                    Log.i("Mohammed", "Port is already open.")
                }
            }
        } catch (e: IOException) {
            Log.e("Mohammed", "Error opening port: ${e.message}", e)
        }
    }
    companion object {
        const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    }
}
