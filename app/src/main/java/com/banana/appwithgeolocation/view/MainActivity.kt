package com.banana.appwithgeolocation.view

import android.Manifest
import android.app.ActivityManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.banana.appwithgeolocation.R
import com.banana.appwithgeolocation.service.LocationService
import com.banana.appwithgeolocation.utils.permissionIsGranted
import com.banana.appwithgeolocation.utils.showToast
import com.banana.appwithgeolocation.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val LOCATION_REQUEST_CODE = 3489
    }

    private lateinit var mViewModel: MainViewModel
    private var mReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        mViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        val host: NavHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment? ?: return

        bottom_nav_view.setupWithNavController(host.navController)

        if (!checkPermissions()) {
            return
        }
        startLocationService()
        setReceiver()

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            unregisterReceiver(mReceiver)
        }
    }

    private fun setReceiver() {
        mReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when(intent?.action) {
                    LocationService.SEND_LOCATION -> setNewLocation(intent)
                    LocationService.SEND_NAME     -> setNewName(intent)
                }
            }
        }
        val intent = IntentFilter()
        intent.addAction(LocationService.SEND_LOCATION)
        intent.addAction(LocationService.SEND_NAME)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mReceiver as BroadcastReceiver, intent)
    }

    private fun setNewLocation(intent: Intent) {
        mViewModel.location.latitude = intent
            .getDoubleExtra("${LocationService.SEND_LOCATION}_LAT", 707.0)
        mViewModel.location.longitude = intent
            .getDoubleExtra("${LocationService.SEND_LOCATION}_LONG", 707.0)
    }

    private fun setNewName(intent: Intent) {
        mViewModel.selectMarker(intent.getStringExtra(LocationService.SEND_NAME) as String)
    }

    private fun startLocationService() {
        if (!isLocationServiceRunning()) {
            val intent = Intent(this, LocationService::class.java)
            intent.action = LocationService.SERVICE_START
            startForegroundService(intent)
        }
    }

    private fun stopLocationService() {
        if (isLocationServiceRunning()) {
            val intent = Intent(this, LocationService::class.java)
            intent.action = LocationService.SERVICE_STOP
            startForegroundService(intent)
        }
    }

    private fun isLocationServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (LocationService::class.java.name == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }
        return false
    }

    private fun checkPermissions(): Boolean {
        if (!Manifest.permission.ACCESS_FINE_LOCATION.permissionIsGranted(this)
            || !Manifest.permission.ACCESS_COARSE_LOCATION.permissionIsGranted(this)) {
            requestPermissions(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), LOCATION_REQUEST_CODE
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                || grantResults[1] == PackageManager.PERMISSION_GRANTED) {
//                mIntentFilter.addAction("SERVICE_GET_LOCATION")
            } else {
                showToast("Разрешения не получены")
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val intent = Intent().apply {
            action = LocationService.UPDATE_SETTINGS
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}
