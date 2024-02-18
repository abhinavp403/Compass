package dev.abhinav.compass

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Looper
import android.util.Log
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

const val REQUEST_LOCATION_PERMISSION: Int = 300
//A callback for receiving notifications from the FusedLocationProviderClient.
lateinit var locationCallback: LocationCallback
//The main entry point for interacting with the Fused Location Provider
lateinit var locationProvider: FusedLocationProviderClient

@Composable
fun CompassScreen() {
    val sensorManager = LocalContext.current.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val sensorEventListener = remember { SensorEventListenerImpl() }

    // Register the sensor listener when the composable is first created
    DisposableEffect(sensorManager) {
        sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)?.let { sensor ->
            sensorManager.registerListener(
                sensorEventListener,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        // Unregister the sensor listener when the composable is disposed
        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    val currentUserLocation = getUserLocation(LocalContext.current)

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.compass_image),
            contentDescription = "compass",
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .rotate(sensorEventListener.currentDegree),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(30.dp))

        SensorDataDisplay(sensorEventListener.currentDegree)

        Spacer(modifier = Modifier.height(30.dp))

        Row {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "location icon",
                tint = Color.Red
            )
            Text(
                text = getReadableLocation(currentUserLocation.latitude, currentUserLocation.longitude, LocalContext.current),
            )
        }
    }
}

@Composable
fun SensorDataDisplay(degree: Float) {
    Text(
        text = "${-degree} degrees",
    )
}

class SensorEventListenerImpl : SensorEventListener {
    // MutableState to hold the degree value
    private val _currentDegree = mutableStateOf(0f)

    // Public read-only property to access the degree values
    var currentDegree: Float by _currentDegree

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        val degree = Math.round(event!!.values[0])
        val rotationAnimation = RotateAnimation(currentDegree, (-degree).toFloat(),
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)

        rotationAnimation.duration = 200
        rotationAnimation.fillAfter = true
        event.values?.let {
            _currentDegree.value = (-degree).toFloat()
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun getUserLocation(context: Context): LatAndLong {
    locationProvider = LocationServices.getFusedLocationProviderClient(context)
    var currentUserLocation by remember { mutableStateOf(LatAndLong()) }

    DisposableEffect(key1 = locationProvider) {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                locationProvider.lastLocation
                    .addOnSuccessListener { location ->
                        location?.let {
                            val lat = location.latitude
                            val long = location.longitude
                            // Update data class with location data
                            currentUserLocation = LatAndLong(latitude = lat, longitude = long)
                        }
                    }
                    .addOnFailureListener {
                        Log.e("Location_error", "${it.message}")
                    }
            }
        }
        if (hasPermissions(context, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            locationUpdate()
        } else {
            askPermissions(context as Activity, REQUEST_LOCATION_PERMISSION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        onDispose {}
    }
    return currentUserLocation
}

fun hasPermissions(context: Context, vararg permissions: String): Boolean {
    for (permission in permissions) {
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
    }
    return true
}

fun askPermissions(activity: Activity, requestCode: Int, vararg permissions: String) {
    if (!hasPermissions(activity, *permissions)) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }
}

@SuppressLint("MissingPermission")
fun locationUpdate() {
    locationCallback.let {
        val locationRequest: LocationRequest =
            LocationRequest().apply {
                interval = TimeUnit.SECONDS.toMillis(60)
                fastestInterval = TimeUnit.SECONDS.toMillis(30)
                maxWaitTime = TimeUnit.MINUTES.toMillis(2)
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
        locationProvider.requestLocationUpdates(locationRequest, it, Looper.getMainLooper())
    }
}

fun getReadableLocation(latitude: Double, longitude: Double, context: Context): String {
    var addressText = ""
    val geocoder = Geocoder(context, Locale.getDefault())
    try {
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        if (addresses?.isNotEmpty() == true) {
            val address = addresses[0]
            addressText = address.locality +  ", " + address.countryName
            Log.d("geolocation", addressText)
        }
    } catch (e: IOException) {
        Log.d("geolocation", e.message.toString())
    }
    return addressText
}
