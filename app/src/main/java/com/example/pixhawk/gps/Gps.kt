import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import com.example.pixhawk.screen.GpsInfoScreen
import com.example.pixhawk.utils.calculateDOP
import com.example.pixhawk.viewModel.LogViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.util.Locale

class Gps(private val context: Context, private val logViewModel: LogViewModel, private val stringOfGpsAndDops: MutableState<StringBuilder>,private val hasLocationEnabled: MutableState<Boolean>,) {
    private val UPDATE_INTERVAL: Long = 10 * 1000
    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val handler = Handler(Looper.getMainLooper())
    private var hdop =  mutableStateOf(0.0)
    private var vdop =  mutableStateOf(0.0)
    private var pdop =  mutableStateOf(0.0)
    var latitude = mutableStateOf(0.0)
    var longitude = mutableStateOf(0.0)
    var altitude = mutableStateOf(0.0)
    var satellites = mutableStateOf(0)
    var speed =  mutableStateOf (0.0)
    var heading = mutableStateOf(0.0)
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, gravity, 0, event.values.size)
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
                }
            }

            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                SensorManager.getOrientation(rotationMatrix, orientation)
                var temoraryAzimuth = Math.toDegrees(orientation[0].toDouble())
                heading.value = String.format("%.1f", temoraryAzimuth).replace(',', '.').toDouble()

                if (heading.value < 0) {
                    heading.value += 360
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    private val locationRunnable = object : Runnable {
        override fun run() {
            getGpsLocation()
            handler.postDelayed(this, UPDATE_INTERVAL)
        }
    }
    init {
        handler.post(locationRunnable)
        sensorManager.registerListener(sensorEventListener, accelerometer, 1)
        sensorManager.registerListener(sensorEventListener, magnetometer, 1)
    }
    @Composable
    fun ShowGpsInformation() {
        GpsInfoScreen(satellites.value,latitude.value,longitude.value,altitude.value,hasLocationEnabled.value,
            hdop.value,vdop.value,pdop.value,speed.value,heading.value)
    }

    private fun getGpsLocation() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        )  {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
            return

        } else{
            hasLocationEnabled.value = true
        }

        if(hasLocationEnabled.value){
            val gnssStatusCallback = object : GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: GnssStatus) {
                    super.onSatelliteStatusChanged(status)

                    satellites.value = status.satelliteCount
                    calculateDOP(status,hdop,vdop,pdop)
                }
            }

            locationManager.registerGnssStatusCallback(gnssStatusCallback, handler)

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult?.lastLocation?.let { location ->

                        latitude.value = location.latitude
                        longitude.value = location.longitude
                        altitude.value = location.altitude
                        val originalSpeedValue = location.speed.toDouble()
                        speed.value = String.format("%.3f", originalSpeedValue).replace(',', '.').toDouble()

                        val formattedLatitude = String.format(Locale.US, "%.6f", latitude.value)
                        val formattedLongitude = String.format(Locale.US, "%.6f", longitude.value )
                        val formattedAltitude = String.format(Locale.US, "%.1f", altitude.value)
                        val formattedHdop = String.format(Locale.US, "%.1f", hdop.value)
                        val formattedVdop = String.format(Locale.US, "%.1f", vdop.value)
                        val formattedPdop = String.format(Locale.US, "%.1f", pdop.value)

                        val gpsData = "Pos: $formattedLatitude,$formattedLongitude,$formattedAltitude,${satellites.value},$formattedHdop,${speed.value},${heading.value}"
                        val dopsData = "DOP: $formattedHdop,$formattedVdop,$formattedPdop,${satellites.value}"

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
    }
}