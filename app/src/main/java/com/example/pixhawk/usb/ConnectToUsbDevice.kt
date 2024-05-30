package com.example.pixhawk.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.compose.runtime.MutableState
import com.example.pixhawk.viewModel.LogViewModel
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import java.io.IOException

fun connectToUsbDevice(
    usbManager: UsbManager,
    usbDevice: UsbDevice,
    driver: UsbSerialDriver?,
    usbSerialPort: MutableState<UsbSerialPort?>,
    connectedToUsb: MutableState<Boolean>,
    logViewModel: LogViewModel
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
            } else {
                logViewModel.addLog("USB device connected, port was already open")
                connectedToUsb.value = true
            }
        }
    } catch (e: IOException) {
        logViewModel.addLog("Error opening port: ${e.message}")
        connectedToUsb.value = false
        usbSerialPort.value = null
    }
}