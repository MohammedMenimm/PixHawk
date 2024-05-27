package com.example.pixhawk

import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import com.example.pixhawk.usb.RequestUsbPermission.Companion.ACTION_USB_PERMISSION
import com.example.pixhawk.viewModel.LogViewModel

class MainActivity : ComponentActivity() {
    private lateinit var usbManager: UsbManager
    private val logViewModel: LogViewModel = LogViewModel()
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usbManager = getSystemService(USB_SERVICE) as UsbManager
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(PixHawk(logViewModel).usbPermissionActionReceiver, filter, RECEIVER_NOT_EXPORTED)

        setContent {
            PixHawk(logViewModel).PixHawkApp()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Unregister the broadcast receiver to prevent memory leaks
        unregisterReceiver(PixHawk(logViewModel).usbPermissionActionReceiver)
    }
}
