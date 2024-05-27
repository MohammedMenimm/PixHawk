package com.example.pixhawk.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class UsbPermissionReceiver(
    private val onPermissionGranted: (UsbDevice) -> Unit,
    private val onPermissionDenied: () -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            RequestUsbPermission.ACTION_USB_PERMISSION -> {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let { onPermissionGranted(it) }
                    } else {
                        onPermissionDenied()
                    }
                }
            }
        }
    }
}
