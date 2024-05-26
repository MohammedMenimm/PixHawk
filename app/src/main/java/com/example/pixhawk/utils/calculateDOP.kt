package com.example.pixhawk.utils

import android.location.GnssStatus
import androidx.compose.runtime.MutableState
import kotlin.math.pow

fun calculateDOP(status: GnssStatus, hdop: MutableState<Double>, vdop:MutableState<Double>, pdop :MutableState<Double>) {
    val numSatellites = status.satelliteCount
    var sumOfInverseSquareSnr = 0.0
    var sumOfSquareElevation = 0.0

    for (i in 0 until numSatellites) {
        val snr = status.getCn0DbHz(i).toDouble()
        val elevationRadians = Math.toRadians(status.getElevationDegrees(i).toDouble())
        val snrLinear = 10.0.pow(snr / 10.0)
        sumOfInverseSquareSnr += 1.0 / snrLinear
        sumOfSquareElevation += Math.sin(elevationRadians).pow(2.0)
    }

    if (numSatellites > 0) {
        val Hdop = Math.sqrt(sumOfInverseSquareSnr)
        val Vdop = Math.sqrt(sumOfSquareElevation / numSatellites)
        val Pdop = Math.sqrt(hdop.value.pow(2.0) + vdop.value.pow(2.0))

        hdop.value= Hdop
        vdop.value = Vdop
        pdop.value = Pdop
    }
}