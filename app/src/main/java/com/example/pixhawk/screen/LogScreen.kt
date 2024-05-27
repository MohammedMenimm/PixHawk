package com.example.pixhawk.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pixhawk.viewModel.LogViewModel
@Composable
fun LogScreen(logViewModel: LogViewModel) {
    val logs by logViewModel.logs.collectAsState()

    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.Center){
        Text( text = "App Logs", modifier = Modifier.padding(bottom = 10.dp), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        LogList(logs = logs)
    }
}
@Composable
fun LogList(logs: List<String>) {
    val reversedLogs = logs.asReversed()
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color = Color.Gray)
            .height(150.dp)
    ) {
        items(reversedLogs) { log ->
            Text(text = log, modifier = Modifier.padding(all = 6.dp),
                color = Color.White, fontSize = 15.sp)
        }
    }
}
