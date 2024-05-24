import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.core.app.ActivityCompat
import com.example.pixhawk.viewModel.LogViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.util.Locale
import kotlin.math.pow

class Gps(private val context: Context, private val logViewModel: LogViewModel, private val stringOfGpsAndDops: MutableState<StringBuilder>) {
    private val UPDATE_INTERVAL: Long = 10 * 100
    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val handler = Handler(Looper.getMainLooper())
    private var hdop = 0.0
    private var vdop = 0.0
    private var pdop = 0.0
    var latitude = 0.0
    var longitude = 0.0
    var altitude = 0.0
    var satellites = 0

    private val locationRunnable = object : Runnable {
        override fun run() {
            getGpsLocation()
            handler.postDelayed(this, UPDATE_INTERVAL)
        }
    }
    init {
        handler.post(locationRunnable)
    }
    private fun getGpsLocation() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
            return
        }

        locationManager.registerGnssStatusCallback(gnssStatusCallback, handler)

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.lastLocation?.let { location ->
                    latitude = location.latitude
                    longitude = location.longitude
                    altitude = location.altitude

                    val formattedLatitude = String.format(Locale.US, "%.6f", latitude)
                    val formattedLongitude = String.format(Locale.US, "%.6f", longitude )
                    val formattedAltitude = String.format(Locale.US, "%.1f", altitude)
                    val formattedHdop = String.format(Locale.US, "%.1f", hdop)
                    val formattedVdop = String.format(Locale.US, "%.1f", vdop)
                    val formattedPdop = String.format(Locale.US, "%.1f", pdop)

                    val gpsData = "Pos: $formattedLatitude,$formattedLongitude,$formattedAltitude,$satellites,$formattedHdop"
                    val dopsData = "DOP: $formattedHdop,$formattedVdop,$formattedPdop,$satellites"

                    Log.i(
                        "Gps",
                        "GPS Location - Latitude: $formattedLatitude, Longitude: $formattedLongitude, Altitude: $formattedAltitude, Satellites: $satellites, HDOP: $formattedHdop, VDOP: $formattedVdop, PDOP: $formattedPdop"
                    )

                    stringOfGpsAndDops.value.clear()
                    stringOfGpsAndDops.value.append("$dopsData\n$gpsData\n")
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            LocationRequest.create().apply {
                interval = UPDATE_INTERVAL
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            },
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            super.onSatelliteStatusChanged(status)

            satellites = status.satelliteCount
            calculateDOP(status)
        }
    }
    private fun calculateDOP(status: GnssStatus) {
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
            val hdop = Math.sqrt(sumOfInverseSquareSnr)
            val vdop = Math.sqrt(sumOfSquareElevation / numSatellites)
            val pdop = Math.sqrt(hdop.pow(2.0) + vdop.pow(2.0))

            this.hdop = hdop
            this.vdop = vdop
            this.pdop = pdop
        }
    }
}