package com.banana.appwithgeolocation.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.banana.appwithgeolocation.R
import com.banana.appwithgeolocation.model.storage.PointRoomDatabase
import com.banana.appwithgeolocation.model.storage.Repository
import com.banana.appwithgeolocation.utils.showToast
import com.banana.appwithgeolocation.view.MainActivity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

class LocationService : IntentService("Location Service") {

    companion object {
        const val SERVICE_START   = "appwithgeolocation.action.SERVICE_START"
        const val SERVICE_STOP    = "appwithgeolocation.action.SERVICE_STOP"
        const val SEND_LOCATION   = "appwithgeolocation.action.SERVICE.SEND_LOCATION"
        const val SEND_NAME       = "appwithgeolocation.action.SERVICE.SEND_NAME"
        const val UPDATE_SETTINGS = "appwithgeolocation.action.SERVICE.UPDATE_SETTINGS"

        const val NOTIFICATION_FOREGROUND_CHANNEL_ID = "appwithgeolocation.notify.channel_id.FOREGROUND"
        const val NOTIFICATION_CHANNEL_ID            = "appwithgeolocation.notify.channel_id.NOTIFICATION"

        const val SETTINGS_ACCURACY    = "appwithgeolocation.settings.ACCURACY"
        const val SETTINGS_SAMPLE_RATE = "appwithgeolocation.settings.SAMPLE_RATE"
    }

    private val repository: Repository = Repository(
        PointRoomDatabase.getDatabase(this).pointDao()
    )

    private var _points: LiveData<List<com.banana.appwithgeolocation.model.entity.Point>> = repository.getPoints()
    private var mPoints = HashMap<Int, com.banana.appwithgeolocation.model.entity.Point>()
    private var mLocation = Location("")
    private var mName = ""
    private var mOldName = ""
    private var mAccuracy: Int = 100
    private var mSampleRate: Long = 60000
    private var mNotifyTicker = 100

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            if (locationResult.lastLocation != null) {
                Log.d("LocationService", "$locationResult")
                Log.d("LocationService", "$mAccuracy")
                Log.d("LocationService", "$mSampleRate")
                mLocation = locationResult.lastLocation
                sendLocation(Intent())
                if (checkDistance(locationResult.lastLocation) && mName != mOldName) {
                    mOldName = mName
                    showNotification()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                SERVICE_START -> startLocationService()
                SERVICE_STOP  -> stopLocationService()
            }
        }
        return Service.START_STICKY
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            when (intent.action) {
                UPDATE_SETTINGS -> setFromSettings()
            }
        }
    }

    private fun sendLocation(intent: Intent?) {
        if (intent != null) {
            intent.apply {
                action = SEND_LOCATION
                putExtra("${SEND_LOCATION}_LAT", mLocation.latitude)
                putExtra("${SEND_LOCATION}_LONG", mLocation.longitude)
            }
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        }
    }

//    private fun sendName(intent: Intent?) {
//        if (intent != null) {
//            intent.apply {
//                action = SEND_NAME
//                putExtra(SEND_NAME, mName)
//            }
//            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
//        }
//    }

    private fun startLocationService() {
        Handler(Looper.getMainLooper()).post {
            _points.observeForever(object : Observer<List<com.banana.appwithgeolocation.model.entity.Point>> {
                override fun onChanged(list: List<com.banana.appwithgeolocation.model.entity.Point>?) {
                    list?.forEach { point ->
                        mPoints[point.id] = point
                    }
                    _points.removeObserver(this)
                }
            })
        }

        setFromSettings()
        showForegroundNotification()
    }

    private fun stopLocationService() {
        LocationServices.getFusedLocationProviderClient(this)
                .removeLocationUpdates(mLocationCallback)
        stopForeground(true)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun setLocationRequest() {
        val locationRequest = LocationRequest()
        locationRequest.interval = mSampleRate
        locationRequest.fastestInterval = mSampleRate
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        LocationServices.getFusedLocationProviderClient(this)
            .requestLocationUpdates(locationRequest, mLocationCallback, Looper.getMainLooper())
    }

    private fun setFromSettings() {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        mAccuracy = settings.getInt(SETTINGS_ACCURACY, 100)
        mSampleRate = settings.getInt(SETTINGS_SAMPLE_RATE, 1).toLong() * 60000
        setLocationRequest()
    }

    private fun checkDistance(location: Location): Boolean {
        mPoints.forEach {
            val pointLocation = Location("")
            pointLocation.latitude = it.value.latitude
            pointLocation.longitude = it.value.longitude
            if (location.distanceTo(pointLocation) <= mAccuracy) {
                mName = it.value.name
                return true
            }
        }
        return false
    }

    private fun showForegroundNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_FOREGROUND_CHANNEL_ID)
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setContentTitle(getString(R.string.app_name))
        builder.setContentText("Location Service is worked")
        builder.setContentIntent(pendingIntent)
        builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        builder.setShowWhen(true)
        builder.setAutoCancel(false)
        builder.priority = NotificationCompat.PRIORITY_MAX
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(NOTIFICATION_FOREGROUND_CHANNEL_ID) == null) {
                val notificationChannel = NotificationChannel(
                    NOTIFICATION_FOREGROUND_CHANNEL_ID,
                    "Location Service",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationChannel.description = "This channel is used by location service #1"
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }
        startForeground(1, builder.build())
    }

    private fun showNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(this, MainActivity::class.java),
            0
        )
        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setContentTitle(getString(R.string.app_name))
        builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        builder.setContentText("You are located near '$mName'")
        builder.setContentIntent(pendingIntent)
        builder.setShowWhen(true)
        builder.setAutoCancel(true)
        builder.priority = NotificationCompat.PRIORITY_MAX
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                val notificationChannel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Location Service",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationChannel.description = "This channel is used by location service #2"
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }
        notificationManager.notify(mNotifyTicker++, builder.build())
    }
}
