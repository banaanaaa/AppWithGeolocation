package com.banana.appwithgeolocation.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.banana.appwithgeolocation.R
import com.banana.appwithgeolocation.service.LocationService
import com.banana.appwithgeolocation.service.ServiceMutableLiveData
import com.banana.appwithgeolocation.utils.*
import com.banana.appwithgeolocation.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val REQUEST_CODE_FINE_COARSE_LOCATION = 34895
        private const val REQUEST_CODE_BACKGROUND_LOCATION  = 32543

        private val PERMISSIONS_LOCATION_FINE_COARSE
                = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION)

        @SuppressLint("InlinedApi")
        private val PERMISSIONS_BACKGROUND_LOCATION
                = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        private const val SHOW_DIALOG_FINE_COARSE_LOCATION = "action.show.location.FINE_COARSE"
        private const val SHOW_DIALOG_BACKGROUND_LOCATION  = "action.show.location.BACKGROUND"
        private const val SHOW_DIALOG_SERVICE_ON           = "action.show.service.ON"

        private const val APP_IS_NOT_FINISHING  = "action.is.FINISHING"
    }

    private lateinit var mViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(view_toolbar)

        mViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        ServiceMutableLiveData.getInstance(this)
            ?.observe(this, Observer<Location> { mViewModel.location = it })

        val host: NavHostFragment = supportFragmentManager
            .findFragmentById(R.id.view_fragment_container) as NavHostFragment? ?: return

        view_bottom_navigation.setupWithNavController(host.navController)

        if (isServiceRunning() && !isServiceOnInSetting()) changeSwitchPreference(true)
        else if (!isServiceRunning() && isServiceOnInSetting()) changeSwitchPreference(false)

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)

        if (!checkPermissions(REQUEST_CODE_FINE_COARSE_LOCATION)) return
        else {
            intent?.let { intent ->
                intent.getBooleanExtra(APP_IS_NOT_FINISHING, false).apply {
                    if (!this && !isServiceRunning()) showDialog(SHOW_DIALOG_SERVICE_ON)
                    intent.removeExtra(APP_IS_NOT_FINISHING)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            it.getBooleanExtra(
                Constants.NOTIFICATION_ACTION_TAP_ON_FOREGROUND,
                false
            ).let { value ->
                if (value) it.removeExtra(Constants.NOTIFICATION_ACTION_TAP_ON_FOREGROUND)
                else it.getStringExtra(Constants.NOTIFICATION_ACTION_TAP_ON_SHORT_NOTIFY)?.let { name ->
                    mViewModel.selectMarker(name)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_FINE_COARSE_LOCATION -> {
                if (checkPermissions(REQUEST_CODE_FINE_COARSE_LOCATION))
                    showDialog(SHOW_DIALOG_SERVICE_ON)
            }
            REQUEST_CODE_BACKGROUND_LOCATION -> background()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isFinishing && !isServiceRunning()) intent.putExtra(APP_IS_NOT_FINISHING, true)
    }

    private fun background() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkPermissions(REQUEST_CODE_BACKGROUND_LOCATION)) {
                Intent(this, LocationService::class.java).apply {
                    action = Constants.SERVICE_START
                    startService(this)
                }
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            Constants.SETTINGS_KEY_SAMPLE_RATE -> if (isServiceRunning()) restartService()
            Constants.SETTINGS_KEY_ACCURACY -> if (isServiceRunning()) restartService()
            Constants.SETTINGS_KEY_SWITCH -> enableService()
        }
    }

    private fun changeSwitchPreference(set: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit().putBoolean(Constants.SETTINGS_KEY_SWITCH, set).apply()
    }

    private fun showDialog(showFor: String) {
        when (showFor) {
            SHOW_DIALOG_FINE_COARSE_LOCATION -> {
                showDialog(
                    getString(R.string.dialog_pref_miss_title),
                    getString(R.string.dialog_pref_miss_desc_background_location),
                    showFor)
            }
            SHOW_DIALOG_BACKGROUND_LOCATION -> {
                showDialog(
                    getString(R.string.dialog_pref_miss_title),
                    getString(R.string.dialog_pref_miss_desc_fine_coarse_location),
                    showFor)
            }
            SHOW_DIALOG_SERVICE_ON -> {
                showDialog(
                    getString(R.string.dialog_service_off_title),
                    getString(R.string.dialog_service_off_desc),
                    showFor,
                    getString(R.string.dialog_button_positive_enable),
                    getString(R.string.dialog_button_negative_cancel)
                )
            }
        }
    }

    private fun showDialog(
        title: String,
        description: String,
        showFor: String,
        positiveButtonText: String = getString(R.string.dialog_button_positive_yes),
        negativeButtonText: String = getString(R.string.dialog_button_negative_no)
    ) {
        createSimpleDialog(
            title,
            createClearLayout(description),
            positiveButtonText,
            negativeButtonText
        ).apply {
            setOnShowListener {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    dismiss()
                    when (showFor) {
                        SHOW_DIALOG_FINE_COARSE_LOCATION -> {
                            showSettings(
                                REQUEST_CODE_FINE_COARSE_LOCATION,
                                getString(R.string.toast_pref_miss_fine_coarse_location)
                            )
                        }
                        SHOW_DIALOG_BACKGROUND_LOCATION -> {
                            showSettings(
                                REQUEST_CODE_BACKGROUND_LOCATION,
                                getString(R.string.toast_pref_miss_background_location)
                            )
                        }
                        SHOW_DIALOG_SERVICE_ON -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                if (checkPermissions(REQUEST_CODE_BACKGROUND_LOCATION))
                                    changeSwitchPreference(true)
                            } else changeSwitchPreference(true)
                        }
                    }
                }
                getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                    if (showFor == SHOW_DIALOG_FINE_COARSE_LOCATION) finish()
                    dismiss()
                }
            }
            show()
        }
    }

    private fun showSettings(requestCode: Int, toastText: String) {
        showToast(toastText, Toast.LENGTH_LONG)
        startActivityForResult(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            ),
            requestCode
        )
    }

    private fun restartService() {
        GlobalScope.launch(Dispatchers.Main) {
            enableOrDisableService(Constants.SERVICE_STOP)
            delay(1500)
            enableOrDisableService(Constants.SERVICE_START)
        }
    }

    private fun enableService() {
        if (isServiceOnInSetting() && !isServiceRunning()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (checkPermissions(REQUEST_CODE_BACKGROUND_LOCATION)) {
                    enableOrDisableService(Constants.SERVICE_START)
                }
            } else enableOrDisableService(Constants.SERVICE_START)
        }
        else if (!isServiceOnInSetting() && isServiceRunning())
            enableOrDisableService(Constants.SERVICE_STOP)
    }

    private fun enableOrDisableService(action: String) {
        Intent(this, LocationService::class.java).apply {
            this.action = action
            startService(this)
        }
    }

    private fun isServiceOnInSetting(): Boolean = PreferenceManager
            .getDefaultSharedPreferences(this)
            .getBoolean(Constants.SETTINGS_KEY_SWITCH, false)

    @SuppressLint("InlinedApi")
    private fun checkPermissions(code: Int): Boolean {
        return when (code) {
            REQUEST_CODE_FINE_COARSE_LOCATION -> {
                if (!PERMISSIONS_LOCATION_FINE_COARSE[0].permissionIsGranted(this)
                    || !PERMISSIONS_LOCATION_FINE_COARSE[1].permissionIsGranted(this)) {
                    requestPermissions(PERMISSIONS_LOCATION_FINE_COARSE, REQUEST_CODE_FINE_COARSE_LOCATION)
                    false
                } else true
            }
            REQUEST_CODE_BACKGROUND_LOCATION -> {
                if (!PERMISSIONS_BACKGROUND_LOCATION[0].permissionIsGranted(this)) {
                    requestPermissions(PERMISSIONS_BACKGROUND_LOCATION, REQUEST_CODE_BACKGROUND_LOCATION)
                    false
                } else true
            }
            else -> false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_FINE_COARSE_LOCATION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                    || grantResults[1] == PackageManager.PERMISSION_GRANTED)
                    showDialog(SHOW_DIALOG_SERVICE_ON)
                else showDialog(SHOW_DIALOG_FINE_COARSE_LOCATION)
            }
            REQUEST_CODE_BACKGROUND_LOCATION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    changeSwitchPreference(true)
                else showDialog(SHOW_DIALOG_BACKGROUND_LOCATION)
            }
        }
    }

}
