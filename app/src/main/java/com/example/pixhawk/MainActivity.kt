package com.example.pixhawk

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import com.example.pixhawk.viewModel.LogViewModel

class MainActivity : ComponentActivity() {
    private val logViewModel: LogViewModel = LogViewModel()
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PixHawk(logViewModel).PixHawkApp()
        }
    }
}
