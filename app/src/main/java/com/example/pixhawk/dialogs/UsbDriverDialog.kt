package com.example.pixhawk.dialogs

import android.content.Context
import android.hardware.usb.UsbManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pixhawk.usb.RequestUsbPermission
import com.example.pixhawk.viewModel.LogViewModel
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

@Composable
fun UsbDriverDialog(context: Context, usbSerialPort: MutableState<UsbSerialPort?>, connectedToUsb: MutableState<Boolean>,
                    alreadyGivenPermission: MutableState<Boolean>, showDialog: MutableState<Boolean>, logViewModel: LogViewModel){
    var driversForDialog by remember { mutableStateOf(listOf<String>()) }
    var availableDrivers by remember { mutableStateOf(emptyList<UsbSerialDriver>()) }
    var selectedDriver by remember { mutableStateOf<UsbSerialDriver?>(null) }
    var selectedDriverIndex by remember { mutableStateOf(-1) }
    val scope = rememberCoroutineScope()
    var alreadyShownDialog by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
                availableDrivers = drivers
                driversForDialog = if (drivers.isEmpty()) {
                    listOf("No USB devices found.")
                    return@launch
                } else {
                    drivers.map { it.device.deviceName }
                }

                if(drivers.size == 1) {
                    selectedDriver = availableDrivers[0]
                    showDialog.value = false
                }
            } catch (e: IOException) {
                logViewModel.addLog("failed to connect to any USB device, ERROR ")
            }
        }
    }

    if (showDialog.value) {
        AlertDialog(onDismissRequest = {!showDialog.value},
            title = { Text(text = "Available USB Drivers") },
            text = { if (driversForDialog.isEmpty()) { Text("Searching for USB devices...") }
            else {
                Column {
                    driversForDialog.forEachIndexed { index, driverName ->
                        Text(driverName, modifier = Modifier.clickable {
                            selectedDriverIndex = index
                            selectedDriver = availableDrivers[index]

                        }.background(if (selectedDriverIndex == index) Color.LightGray else Color.Transparent)
                            .padding(8.dp),
                            color = if (selectedDriverIndex == index) Color.Black else Color.Unspecified
                        )
                    }
                }
            }
            },
            confirmButton = { Button(
                onClick = {
                    showDialog.value = false
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray,
                    contentColor = Color.Black
                )
            ) {
                Text("OK")
            }
            },
            dismissButton = { Button(
                onClick = {
                    selectedDriver = null
                    showDialog.value = false
                          return@Button},
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray,
                    contentColor = Color.Black
                )
            ) {
                Text("Cancel")
            }
            }
        )
    }

    if (!showDialog.value && !alreadyShownDialog && !alreadyGivenPermission.value) {
        logViewModel.addLog("Requesting USB permission")
        alreadyShownDialog = true
        RequestUsbPermission(logViewModel)
            .requestUsbPermission(context, selectedDriver, usbSerialPort, connectedToUsb,alreadyGivenPermission
        )
    }
}