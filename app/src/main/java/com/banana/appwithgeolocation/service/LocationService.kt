package com.banana.appwithgeolocation.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.banana.appwithgeolocation.R
import com.banana.appwithgeolocation.view.MainActivity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.lang.UnsupportedOperationException

class LocationService : Service() {

    private var mPoints = HashMap<Int, com.banana.appwithgeolocation.model.entity.Point>()
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            if (locationResult.lastLocation != null) {
                val name = checkDistance(locationResult.lastLocation)
                if (name != "") {
                    showNotification(name)
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        throw UnsupportedOperationException("Not yet implemented")
    }

    fun checkDistance(location: Location): String {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val accuracy = settings.getInt("accuracy", 100)
        mPoints.forEach { point ->
            val pointLocation = Location("")
            pointLocation.latitude = point.value.latitude
            pointLocation.longitude = point.value.longitude
            if (location.distanceTo(pointLocation) <= accuracy) {
                return point.value.name
            }
        }
        return ""
    }

    private fun showNotification(name: String) {
        val channelId = "location_notification_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            0
        )
        val builder = NotificationCompat.Builder(applicationContext, channelId)
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setContentTitle(getString(R.string.app_name))
        builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        builder.setContentText("You are located near ${name
                .padStart(1, '"')
                .padEnd(1, '"')}")
        builder.setContentIntent(pendingIntent)
        builder.setShowWhen(true)
        builder.setAutoCancel(true)
        builder.setTimeoutAfter(60000)
        builder.priority = NotificationCompat.PRIORITY_MAX
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val notificationChannel = NotificationChannel(
                    channelId,
                    "Location Service",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationChannel.description = "This channel is used by location service"
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }
        notificationManager.notify(1, builder.build())
//        startForeground(100, builder.build())
    }

    @SuppressLint("MissingPermission")
    private fun startLocationService() {
        val locationRequest = LocationRequest()
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val interval = settings.getInt("sample_rate", 1).toLong()
        locationRequest.interval = interval * 60000
        locationRequest.fastestInterval = interval * 30000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        LocationServices.getFusedLocationProviderClient(this)
                .requestLocationUpdates(locationRequest, mLocationCallback, Looper.getMainLooper())
    }

    private fun stopLocationService() {
        Toast.makeText(this,"Stop", Toast.LENGTH_SHORT).show()
        LocationServices.getFusedLocationProviderClient(this)
                .removeLocationUpdates(mLocationCallback)
        stopForeground(true)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            if (action != null) {
                if (action == "ACTION_START_SERVICE") {
                    val i = intent.getIntExtra("iterator", 0)
                    for (k in 0..i) {
                        val lat = intent.getDoubleExtra("pointLat$k", 707.0)
                        val long = intent.getDoubleExtra("pointLong$k", 707.0)
                        val name = intent.getStringExtra("pointName$k") as String
                        val point = com.banana.appwithgeolocation.model.entity.Point(
                            name, lat, long
                        )
                        if (lat != 707.0 || long != 707.0 || name.length > 5) {
                            mPoints[k] = point
                        }
                    }
                    startLocationService()
                } else if (action == "ACTION_STOP_SERVICE") {
                    stopLocationService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
}
