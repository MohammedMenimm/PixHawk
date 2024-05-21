package com.example.pixhawk

import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import com.example.pixhawk.ui.theme.PixHawk
import com.example.pixhawk.ui.theme.PixHawk.Companion.ACTION_USB_PERMISSION


class MainActivity : ComponentActivity() {
    private lateinit var usbManager: UsbManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usbManager = getSystemService(USB_SERVICE) as UsbManager
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(PixHawk().usbPermissionActionReceiver, filter, RECEIVER_NOT_EXPORTED)
        setContent {
            PixHawk().FakeGPSApp()
        }
    }
}
