package com.example.pixhawk.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun PixHawkHomeScreen(connectedToUsb: MutableState<Boolean>, transmittingData: MutableState<Boolean>) {
    Column(modifier = Modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally){
        Text(modifier = Modifier.padding(top = 30.dp, bottom = 20.dp), text = "PixHawk", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Absolute.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(modifier = Modifier.padding( bottom = 5.dp),text = "Connected",fontSize = 15.sp, fontWeight = FontWeight.Bold)
                SendingOrTransmittingIndicator(state = connectedToUsb)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(modifier = Modifier.padding(bottom = 5.dp),text = "Transmitting",fontSize = 15.sp, fontWeight = FontWeight.Bold)
                SendingOrTransmittingIndicator(state = transmittingData)
            }
        }
    }
}
@Composable
fun SendingOrTransmittingIndicator(state: MutableState<Boolean>) {
    val color = if (state.value) Color.Green else Color.Red
    Box(
        modifier = Modifier
            .size(50.dp)
            .background(color, shape = CircleShape)
    )
}