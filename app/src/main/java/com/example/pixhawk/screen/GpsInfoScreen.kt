package com.example.pixhawk.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import java.util.Locale

@Composable
fun GpsInfoScreen(satellites: Int, latitude: Double, longitude : Double, altitude : Double, hasLocationEnabled: Boolean,
                  hdop: Double, vdop: Double, pdop: Double, speed : Double, bearing : Double
) {

    val formattedLatitude = String.format(Locale.US, "%.6f", latitude)
    val formattedLongitude = String.format(Locale.US, "%.6f", longitude )
    val formattedAltitude = String.format(Locale.US, "%.1f", altitude)
    val formattedHdop = String.format(Locale.US, "%.1f", hdop)
    val formattedVdop = String.format(Locale.US, "%.1f", vdop)
    val formattedPdop = String.format(Locale.US, "%.1f", pdop)

    Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.Center){
        Text( text = "Gps Information", modifier = Modifier.padding(bottom = 10.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(color = Color.Gray)
                .height(200.dp)
        ) {
            Row {
                Text( text = "Satelites : $satellites", modifier = Modifier.padding(start = 5.dp, bottom = 10.dp), fontSize = 20.sp, color = Color.White)
                Spacer(modifier = Modifier.weight(1f))
                Text( text = "Hdop : $formattedHdop", modifier = Modifier.padding(bottom = 10.dp, end = 10.dp), fontSize = 20.sp, color = Color.White)
            }
            Row {
                Text( text = "Latitude : $formattedLatitude", modifier = Modifier.padding(start = 5.dp,bottom = 10.dp), fontSize = 20.sp, color = Color.White)
                Spacer(modifier = Modifier.weight(1f))
                Text( text = "Vdop : $formattedVdop", modifier = Modifier.padding(bottom = 10.dp, end = 10.dp), fontSize = 20.sp, color = Color.White)
            }
            Row {
                Text( text = "Longitude : $formattedLongitude", modifier = Modifier.padding(start = 5.dp,bottom = 10.dp), fontSize = 20.sp, color = Color.White)
                Spacer(modifier = Modifier.weight(1f))
                Text( text = "Pdop : $formattedPdop", modifier = Modifier.padding(bottom = 10.dp, end = 10.dp), fontSize = 20.sp, color = Color.White)
            }

            Row {
                Text( text = "Altitude : $formattedAltitude", modifier = Modifier.padding(start = 5.dp,bottom = 10.dp), fontSize = 20.sp, color = Color.White)
                Spacer(modifier = Modifier.weight(1f))
                Text( text = "Speed : $speed", modifier = Modifier.padding(bottom = 10.dp, end = 10.dp), fontSize = 20.sp, color = Color.White)
            }
            Row {
                Text( text = "Location : $hasLocationEnabled", modifier = Modifier.padding(start = 5.dp,bottom = 10.dp), fontSize = 20.sp, color = Color.White)
                Spacer(modifier = Modifier.weight(1f))
                Text( text = "Bearing : $bearing", modifier = Modifier.padding(start = 5.dp,end = 10.dp), fontSize = 20.sp, color = Color.White)

            }
        }
    }
}