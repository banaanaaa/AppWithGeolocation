package com.banana.appwithgeolocation.view

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.banana.appwithgeolocation.R
import com.banana.appwithgeolocation.model.entity.Point
import com.banana.appwithgeolocation.service.LocationService
import com.banana.appwithgeolocation.utils.showToast
import com.banana.appwithgeolocation.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var mViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        mViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        val host: NavHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment? ?: return

        bottom_nav_view.setupWithNavController(host.navController)

        stopLocationService()
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

    private fun startLocationService() {
        if (!isLocationServiceRunning()) {
            val intent = Intent(this, LocationService::class.java)
            intent.action = "ACTION_START_SERVICE"
            var i = 0
            mViewModel.points.value?.forEach {
                intent.putExtra("pointLat$i", it.latitude)
                intent.putExtra("pointLong$i", it.longitude)
                intent.putExtra("pointName$i", it.name)
                i++
            }
            intent.putExtra("iterator", i)
            startService(intent)
        }
    }

    private fun stopLocationService() {
        if (isLocationServiceRunning()) {
            val intent = Intent(this, LocationService::class.java)
            intent.action = "ACTION_STOP_SERVICE"
            startService(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            startLocationService()
        }
    }
}
