package com.banana.appwithgeolocation.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
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
    }

    private lateinit var mViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        mViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        ServiceMutableLiveData.getInstance(this)
            ?.observe(this, Observer<Location> { mViewModel.location = it })

        val host: NavHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment? ?: return

        bottom_nav_view.setupWithNavController(host.navController)

        when (isServiceRunning()) {
            true -> {
                if (isServiceOnInSetting()) setPreferencesListener()
                else changeSwitchPreference(true)
            }
            false -> {
                if (isServiceOnInSetting()) changeSwitchPreference(false)
                else setPreferencesListener()
            }
        }

        if (!checkPermissions(REQUEST_CODE_FINE_COARSE_LOCATION)) return
        else if (!isServiceRunning()) showDialog(SHOW_DIALOG_SERVICE_ON)
    }

    private fun setPreferencesListener() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            it.getBooleanExtra(Constants.NOTIFICATION_ACTION_TAP_ON_FOREGROUND, false).let { value ->
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
                    "Отсутствуют необходимые разрешения", // заменить
                    "Отсутствуют разрешения для получения текущего местоположения в фоновом режиме. Вы не будете получать уведомления о приближении к сохранённым 'точкам'.\n\nПерейти в настройки?", // заменить
                    showFor)
            }
            SHOW_DIALOG_BACKGROUND_LOCATION -> {
                showDialog(
                    "Отсутствуют необходимые разрешения", // заменить
                    "Отсутствуют разрешения для получения текущего местоположения. Если вы не предоставите их, приложение не будет работать.\n\nПерейти в настройки?",
                    showFor)
            }
            SHOW_DIALOG_SERVICE_ON -> {
                showDialog(
                    "Сервис выключен", // заменить
                    "В данный момент выключен сервис для получения текущего местоположения. Без него Вы не будете получать текущее местоположение.\n\nВключить?",  // заменить
                    showFor)
            }
        }
    }

    private fun showDialog(
        title: String,
        description: String,
        showFor: String
    ) {
        createSimpleDialog(
            title,
            createClearLayout(description),
            getString(R.string.yes),
            getString(R.string.no)
        ).apply {
            setOnShowListener {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    dismiss()
                    when (showFor) {
                        SHOW_DIALOG_FINE_COARSE_LOCATION -> {
                            showSettings(REQUEST_CODE_FINE_COARSE_LOCATION, "Предоставьте доступ к местоположению")       // заменить
                        }
                        SHOW_DIALOG_BACKGROUND_LOCATION -> {
                            showSettings(REQUEST_CODE_BACKGROUND_LOCATION, "Предоставьте доступ к местоположению в фоновом режиме")       // заменить
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
                    when (showFor) {
                        SHOW_DIALOG_FINE_COARSE_LOCATION -> {
                            finish()
                        }
                        SHOW_DIALOG_BACKGROUND_LOCATION -> {
                        }
                        SHOW_DIALOG_SERVICE_ON -> {
                        }
                    }
                    dismiss()
                }
            }
            show()
        }
    }

    private fun showSettings(requestCode: Int, toastText: String) {
        showToast(toastText, Toast.LENGTH_LONG)
        startActivityForResult(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
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
        else if (!isServiceOnInSetting() && isServiceRunning()) enableOrDisableService(Constants.SERVICE_STOP)
    }

    private fun enableOrDisableService(action: String) {
        Intent(this, LocationService::class.java).apply {
            this.action = action
            startService(this)
        }
    }

    private fun isServiceRunning(): Boolean {
        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).apply {
            for (service in this.getRunningServices(Int.MAX_VALUE)) {
                if (LocationService::class.java.name == service.service.className) {
                    if (service.foreground) return true
                }
            }
        }
        return false
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
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_FINE_COARSE_LOCATION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                    || grantResults[1] == PackageManager.PERMISSION_GRANTED) showDialog(SHOW_DIALOG_SERVICE_ON)
                else showDialog(SHOW_DIALOG_FINE_COARSE_LOCATION)
            }
            REQUEST_CODE_BACKGROUND_LOCATION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) changeSwitchPreference(true)
                else showDialog(SHOW_DIALOG_BACKGROUND_LOCATION)
            }
        }
    }
}
