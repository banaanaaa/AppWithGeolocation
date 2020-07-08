package com.banana.appwithgeolocation.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.banana.appwithgeolocation.R
import com.banana.appwithgeolocation.model.storage.PointRoomDatabase
import com.banana.appwithgeolocation.model.storage.Repository
import com.banana.appwithgeolocation.utils.Constants
import com.banana.appwithgeolocation.view.MainActivity
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationServices.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationService : Service() {

    private lateinit var points: LiveData<List<com.banana.appwithgeolocation.model.entity.Point>>
    private var mMutableLiveData = ServiceMutableLiveData.getInstance(this)
    private var mPoints = HashMap<Int, com.banana.appwithgeolocation.model.entity.Point>()
    private var mPointsListener = Observer<List<com.banana.appwithgeolocation.model.entity.Point>> {
        mPoints.clear()
        it.forEach { point -> mPoints[point.id] = point }
    }

    private var mAccuracy = 100
    private var mSampleRate = 60000L
    private var mName = ""

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            GlobalScope.launch(Dispatchers.Main) {
                delay(1000)
                locationResult.lastLocation?.let { location ->
                    checkDistance(location).let { name ->
                        if (name != "" && name != mName) {
                            mName = name
                            showNotification(NotificationManager.IMPORTANCE_DEFAULT)
                        }
                    }
                    mMutableLiveData?.value = location
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                Constants.SERVICE_START -> startService()
                Constants.SERVICE_STOP  -> stopService()
                Constants.NOTIFICATION_ACTION_STOP_SERVICE -> {
                    changeSwitchPreferenceToFalse()
                    stopService()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startService() {
        Repository(PointRoomDatabase.getDatabase(this).pointDao()).apply {
            points = this.getPoints().apply {
                Handler(Looper.getMainLooper()).post { observeForever(mPointsListener) }
            }
        }

        PreferenceManager.getDefaultSharedPreferences(this).apply {
            mAccuracy = getInt(Constants.SETTINGS_KEY_ACCURACY, 100)
            mSampleRate = getInt(Constants.SETTINGS_KEY_SAMPLE_RATE, 1).toLong() * 60000
        }

        if (ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            getFusedLocationProviderClient(this).requestLocationUpdates(
                LocationRequest().apply {
                    interval = mSampleRate
                    fastestInterval = mSampleRate / 6
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                },
                mLocationCallback,
                Looper.getMainLooper()
            )
            showNotification(NotificationManager.IMPORTANCE_HIGH, showInForeground = true, autoCancel = false)
        }
    }

    private fun stopService() {
        points.removeObserver(mPointsListener)
        getFusedLocationProviderClient(this).removeLocationUpdates(mLocationCallback)
        stopForeground(true)
        stopSelf()
    }

    private fun changeSwitchPreferenceToFalse() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putBoolean(Constants.SETTINGS_KEY_SWITCH, false).apply()
    }

    private fun checkDistance(location: Location): String {
        HashMap<String, Float>().let { map ->
            mPoints.forEach { point ->
                Location("").apply {
                    latitude = point.value.latitude
                    longitude = point.value.longitude
                }.let { pointLocation ->
                    location.distanceTo(pointLocation).let { distance ->
                        if (distance <= mAccuracy) map[point.value.name] = distance
                    }
                }
            }
            return if (map.isNotEmpty()) map.minBy { (_, distance) -> distance }?.key as String
                   else ""
        }
    }

    private fun showNotification(
        importance: Int, showInForeground: Boolean = false, autoCancel: Boolean = true
    ) {
        val channelId = if (showInForeground) Constants.NOTIFICATION_CHANNEL_ID_FOREGROUND
                        else Constants.NOTIFICATION_CHANNEL_ID_SHORT_NOTIFY

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel(
                notificationManager,
                channelId,
                if (showInForeground) getString(R.string.service_channel_desc_foreground)
                else getString(R.string.service_channel_desc_short_notify),
                importance
            )

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            if (showInForeground) 14323
            else 32857,
            Intent(this, MainActivity::class.java).apply {
                if (showInForeground) {
                    putExtra(Constants.NOTIFICATION_ACTION_TAP_ON_FOREGROUND, true)
                } else {
                    putExtra(Constants.NOTIFICATION_ACTION_TAP_ON_SHORT_NOTIFY, mName)
                }
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        NotificationCompat.Builder(this, channelId).apply {
            setSmallIcon(R.mipmap.ic_launcher)
//            setContentTitle(getString(R.string.app_name))
            setContentText(
                if (showInForeground) getString(R.string.notification_foreground_text)
                else getString(R.string.notification_short_notify_text, mName)
            )
            setContentIntent(pendingIntent)
            setShowWhen(true)
            setAutoCancel(autoCancel)

            priority = NotificationCompat.PRIORITY_MAX

            if (showInForeground) {
                addAction(NotificationCompat.Action(
                    android.R.drawable.ic_media_pause,
                    getString(R.string.notification_button_stop_service),
                    PendingIntent.getService(
                        applicationContext,
                        34534,
                        Intent(this@LocationService, LocationService::class.java)
                                .apply { action = Constants.NOTIFICATION_ACTION_STOP_SERVICE },
                        0
                    )
                ))
                startForeground(channelId.hashCode(), build())
            } else notificationManager.notify(channelId.hashCode(), build())
        }
    }

    private fun createChannel(
        manager: NotificationManager, channelId: String, description: String, importance: Int
    ) {
        NotificationChannel(channelId, "LocationService: $channelId", importance).apply {
            setDescription(description)
            enableLights(true)
            lightColor = Color.BLUE
            manager.createNotificationChannel(this)
        }
    }

}
