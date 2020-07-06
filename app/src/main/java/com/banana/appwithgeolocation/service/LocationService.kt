package com.banana.appwithgeolocation.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.banana.appwithgeolocation.R
import com.banana.appwithgeolocation.model.storage.PointRoomDatabase
import com.banana.appwithgeolocation.model.storage.Repository
import com.banana.appwithgeolocation.utils.Constants
import com.banana.appwithgeolocation.view.MainActivity
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationService : Service() {

    private lateinit var repository: Repository
    private lateinit var points: LiveData<List<com.banana.appwithgeolocation.model.entity.Point>>

    private var mPoints = HashMap<Int, com.banana.appwithgeolocation.model.entity.Point>()
    private var mPointsListener = Observer<List<com.banana.appwithgeolocation.model.entity.Point>> { list ->
        mPoints.clear()
        list?.forEach { mPoints[it.id] = it }
    }

    private var mMutableLiveData = ServiceMutableLiveData.getInstance(this)

    private var mAccuracy: Int = 100
    private var mSampleRate: Long = 60000

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            GlobalScope.launch(Dispatchers.Main) {
                delay(1000)
                locationResult.lastLocation?.let { location ->
                    checkDistance(location).let { name ->
                        if (name != "") showNotification(name)
                    }
                    mMutableLiveData?.value = location
                    Log.d("LocationService", "$location")
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                Constants.SERVICE_START -> startLocationService()
                Constants.SERVICE_STOP  -> stopLocationService()
                "StopFromButton" -> stopLocationServiceFromButton()
            }
        }
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun stopLocationServiceFromButton() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putBoolean(Constants.SETTINGS_KEY_SWITCH, false)
            .apply()

        points.removeObserver(mPointsListener)
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(mLocationCallback)
        stopForeground(true)
        stopSelf()
    }

    private fun setPointsListener() {
        repository = Repository(PointRoomDatabase.getDatabase(this).pointDao())
        points = repository.getPoints()
        Handler(Looper.getMainLooper()).post {
            points.observeForever(mPointsListener)
        }
    }

    private fun startLocationService() {
        setPointsListener()
        updateSettings()
        setLocationRequest()
        showForegroundNotification()
    }

    private fun stopLocationService() {
        points.removeObserver(mPointsListener)
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(mLocationCallback)
        stopForeground(true)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun setLocationRequest() {
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(mLocationCallback)

        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(
            LocationRequest().apply {
                interval = mSampleRate
                fastestInterval = mSampleRate / 6
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            },
            mLocationCallback,
            Looper.getMainLooper()
        )
    }

    private fun updateSettings() {
        PreferenceManager.getDefaultSharedPreferences(this).apply {
            mAccuracy = getInt(Constants.SETTINGS_KEY_ACCURACY, 100)
            mSampleRate = getInt(Constants.SETTINGS_KEY_SAMPLE_RATE, 1).toLong() * 60000
        }
    }

    private fun checkDistance(location: Location): String {
        HashMap<String, Float>().let { map ->
            mPoints.forEach {point ->
                Location("").apply {
                    latitude = point.value.latitude
                    longitude = point.value.longitude
                }.let { pointLocation ->
                    location.distanceTo(pointLocation).let { distance ->
                        if (distance <= mAccuracy) map[point.value.name] = distance
                    }
                }
            }
            return if (map.isNotEmpty()) {
                val min =  map.minBy { (_, distance) -> distance }
                min?.key as String
            } else ""
        }
    }

    private fun showForegroundNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                createChannel(
                    notificationManager,
                    Constants.NOTIFICATION_CHANNEL_ID_FOREGROUND,
                    "This channel for Foreground Notify of Location Service",
                    NotificationManager.IMPORTANCE_HIGH
                )

        val pendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    0,
                    Intent(this, MainActivity::class.java).apply {
                        putExtra(Constants.NOTIFICATION_ACTION_TAP_ON_FOREGROUND, true)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

        val intentButton = Intent(this, LocationService::class.java)
                .apply {
                    action = "StopFromButton"
                }

        val builder = NotificationCompat.Builder(
            this,
            Constants.NOTIFICATION_CHANNEL_ID_FOREGROUND
        ).apply {
            setSmallIcon(R.mipmap.ic_launcher)
            setContentTitle(getString(R.string.app_name))
            setContentText("Location Service is worked")
            setContentIntent(pendingIntent)
            setDefaults(NotificationCompat.DEFAULT_ALL)
            setShowWhen(true)
            setAutoCancel(false)

            priority = NotificationCompat.PRIORITY_MAX

            addAction(NotificationCompat.Action(
                    android.R.drawable.ic_media_pause,
                    "Stop service",
                    PendingIntent.getService(applicationContext, 0, intentButton, 0)
                )
            )
        }

        startForeground(Constants.NOTIFICATION_CHANNEL_ID_FOREGROUND.hashCode(), builder.build())
    }


    private fun showNotification(name: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel(
                notificationManager,
                Constants.NOTIFICATION_CHANNEL_ID_SHORT_NOTIFY,
                "This channel for Short Notify of Location Service",
                NotificationManager.IMPORTANCE_HIGH
            )

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            1,
            Intent(this, MainActivity::class.java).apply {
                putExtra(Constants.NOTIFICATION_ACTION_TAP_ON_SHORT_NOTIFY, name)
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(
            this,
            Constants.NOTIFICATION_CHANNEL_ID_SHORT_NOTIFY
        ).apply {
            setSmallIcon(R.mipmap.ic_launcher)
            setContentTitle(getString(R.string.app_name))
            setDefaults(NotificationCompat.DEFAULT_ALL)
            setContentText("You are located near '$name'")
            setContentIntent(pendingIntent)
            setShowWhen(true)
            setAutoCancel(true)

            priority = NotificationCompat.PRIORITY_MAX
        }

        notificationManager.notify(Constants.NOTIFICATION_CHANNEL_ID_SHORT_NOTIFY.hashCode(), builder.build())
    }

    private fun createChannel(
        manager: NotificationManager,
        channelId: String,
        description: String,
        importance: Int
    ) {
        NotificationChannel(
            channelId,
            "LocationService: $channelId",
            importance
        ).apply {
            setDescription(description)
            enableLights(true)
            lightColor = Color.BLUE
            manager.createNotificationChannel(this)
        }
    }
}
