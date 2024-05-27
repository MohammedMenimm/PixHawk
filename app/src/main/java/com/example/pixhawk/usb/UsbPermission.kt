package com.example.pixhawk.usb

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.example.pixhawk.viewModel.LogViewModel
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort

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
            connectToUsbDevice(usbManager, usbDevice, driver, usbSerialPort,connectedToUsb, logViewModel)
        } else {
            // Request permission
            val permissionIntent = PendingIntent.getBroadcast(context, 0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            val receiver = UsbPermissionReceiver(
                onPermissionGranted = { device ->
                    alreadyGivenPermission.value = true
                    connectToUsbDevice(usbManager, device, driver, usbSerialPort,connectedToUsb,logViewModel)
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
    companion object {
        const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    }
}
