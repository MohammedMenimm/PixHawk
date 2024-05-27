package com.example.pixhawk.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GpsInfoScreen(satellites: Int, latitude: Double, longitude : Double, altitude : Double, hasLocationEnabled: Boolean) {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.Center){
        Text( text = "Gps Information", modifier = Modifier.padding(bottom = 10.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(color = Color.Gray)
                .height(200.dp)
        ) {
            Text( text = "Satelites : $satellites", modifier = Modifier.padding(start = 5.dp, bottom = 10.dp), fontSize = 20.sp, color = Color.White)
            Text( text = "Latitude : $latitude", modifier = Modifier.padding(start = 5.dp,bottom = 10.dp), fontSize = 20.sp, color = Color.White)
            Text( text = "Longitude : $longitude", modifier = Modifier.padding(start = 5.dp,bottom = 10.dp), fontSize = 20.sp, color = Color.White)
            Text( text = "Altitude : $altitude", modifier = Modifier.padding(start = 5.dp,bottom = 10.dp), fontSize = 20.sp, color = Color.White)
            Text( text = "Location Enabled : $hasLocationEnabled", modifier = Modifier.padding(start = 5.dp,bottom = 10.dp), fontSize = 20.sp, color = Color.White)
        }
    }
}