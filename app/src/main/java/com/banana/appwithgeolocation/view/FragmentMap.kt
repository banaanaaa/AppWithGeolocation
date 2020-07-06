package com.banana.appwithgeolocation.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.banana.appwithgeolocation.R
import com.banana.appwithgeolocation.model.entity.Point
import com.banana.appwithgeolocation.utils.Constants
import com.banana.appwithgeolocation.utils.createSimpleDialog
import com.banana.appwithgeolocation.utils.showToast
import com.banana.appwithgeolocation.viewmodel.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.dialog_input.view.*
import kotlinx.android.synthetic.main.dialog_new_point.view.*
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FragmentMap : Fragment(), OnMapReadyCallback {

    private lateinit var mViewModel: MainViewModel
    private lateinit var mMap: GoogleMap
    private var mSelectedPointListener = Observer<String> { if (it != "") drawMarkers(it, true) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).apply {
            getMapAsync(this@FragmentMap)
        }

        buttonAddPosition.setOnClickListener {
            if (checkDistance()) showDialog()
            else requireActivity().showToast("Рядом уже есть точка")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        mMap = googleMap.apply {
            isMyLocationEnabled = true
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isMapToolbarEnabled = false

            setOnMapLongClickListener {
                if (checkDistance()) showDialog(Location("").apply {
                    latitude = it.latitude
                    longitude = it.longitude
                })
                else requireActivity().showToast("Рядом уже есть точка")
            }
        }

        var name = ""
        requireActivity().intent.getStringExtra(Constants.NOTIFICATION_ACTION_TAP_ON_SHORT_NOTIFY)?.let {
            name = it
            requireActivity().intent.removeExtra(Constants.NOTIFICATION_ACTION_TAP_ON_SHORT_NOTIFY)
        }
        if (name == "") {
            mViewModel.selectedPointName.value?.let {
                if (it != "") name = it
            }
        }

        mViewModel.selectedPointName.observe(this, mSelectedPointListener)

        drawMarkers(name)
    }

    private fun drawMarkers(name: String, fromNotification: Boolean = false) {
        mViewModel.points.observe(this@FragmentMap, Observer { list ->
            mMap.clear()
            if (name != "") {
                list.forEach {
                    if (it.name == name) {
                        addMarker(it, true)
                        moveCameraOnPoint(it)
                    } else addMarker(it)
                }
                mViewModel.selectMarker("")
            }
            else {
                list.forEach { addMarker(it) }
                if (mViewModel.location.hasAltitude()) {
                    moveOnLastLocation()
                } else {
                    if (list.isNotEmpty()) moveCameraOnLocation(list.last().latitude, list.last().longitude)
                }
            }
        })
        if (fromNotification) mViewModel.selectedPointName.removeObserver(mSelectedPointListener)
    }

    private fun addMarker(point: Point, selected: Boolean = false) {
        mMap.addMarker(
            MarkerOptions()
                    .position(LatLng(point.latitude, point.longitude))
                    .title(point.name)
        ).setIcon(BitmapDescriptorFactory.defaultMarker(
            if (selected) BitmapDescriptorFactory.HUE_BLUE
            else BitmapDescriptorFactory.HUE_RED
        ))
    }

    private fun checkDistance() = mViewModel.checkDistance(PreferenceManager
            .getDefaultSharedPreferences(requireActivity())
            .getInt(Constants.SETTINGS_KEY_ACCURACY, 100))

    @SuppressLint("InflateParams")
    private fun showDialog() {
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_new_point, null)
        val name = dialogLayout.name_edittext.text
        val layout = dialogLayout.input_layout

        dialogLayout.apply {
            latitude_textview.text = getString(R.string.dialog_new_point_latitude,
                    mViewModel.location.latitude)
            longitude_textview.text = getString(R.string.dialog_new_point_longitude,
                    mViewModel.location.longitude)
        }

        requireActivity().createSimpleDialog(
            getString(R.string.dialog_new_point_title),
            dialogLayout,
            getString(R.string.ok),
            getString(R.string.cancel)
        ).apply {
            setOnShowListener {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val point = Point(
                        name.toString(),
                        mViewModel.location.latitude,
                        mViewModel.location.longitude
                    )
                    when (mViewModel.addPoint(point)) {
                        MainViewModel.NameValidationResult.TOO_SHORT -> {
                            layout.error = getString(R.string.error_name_is_short)
                            requireActivity().showToast(getString(R.string.toast_is_short))
                        }
                        MainViewModel.NameValidationResult.TOO_LONG -> {
                            layout.error = getString(R.string.error_name_is_long)
                            requireActivity().showToast(getString(R.string.toast_is_long))
                        }
                        MainViewModel.NameValidationResult.ALREADY_EXISTS -> {
                            layout.error = getString(R.string.error_name_is_exist)
                            requireActivity().showToast(getString(R.string.toast_is_exist))
                        }
                        MainViewModel.NameValidationResult.SUCCESS -> {
                            dismiss()
                            GlobalScope.launch(Dispatchers.Main) {
                                moveCameraOnLocation(point.latitude, point.longitude)
                            }
                        }
                    }
                }
            }
            show()
        }
    }

    @SuppressLint("InflateParams")
    private fun showDialog(location: Location) {
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_new_point, null)
        val name = dialogLayout.name_edittext.text
        val layout = dialogLayout.input_layout

        dialogLayout.apply {
            latitude_textview.text = getString(R.string.dialog_new_point_latitude,
                location.latitude)
            longitude_textview.text = getString(R.string.dialog_new_point_longitude,
                location.longitude)
        }

        requireActivity().createSimpleDialog(
            getString(R.string.dialog_new_point_title),
            dialogLayout,
            getString(R.string.ok),
            getString(R.string.cancel)
        ).apply {
            setOnShowListener {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val point = Point(name.toString(), location.latitude, location.longitude)
                    when (mViewModel.addPoint(point)) {
                        MainViewModel.NameValidationResult.TOO_SHORT -> {
                            layout.error = getString(R.string.error_name_is_short)
                            requireActivity().showToast(getString(R.string.toast_is_short))
                        }
                        MainViewModel.NameValidationResult.TOO_LONG -> {
                            layout.error = getString(R.string.error_name_is_long)
                            requireActivity().showToast(getString(R.string.toast_is_long))
                        }
                        MainViewModel.NameValidationResult.ALREADY_EXISTS -> {
                            layout.error = getString(R.string.error_name_is_exist)
                            requireActivity().showToast(getString(R.string.toast_is_exist))
                        }
                        MainViewModel.NameValidationResult.SUCCESS -> {
                            dismiss()
                            GlobalScope.launch(Dispatchers.Main) {
                                moveCameraOnLocation(point.latitude, point.longitude)
                            }
                        }
                    }
                }
            }
            show()
        }
    }

    private fun moveCameraOnPoint(point: Point) {
        moveCameraOnLocation(point.latitude, point.longitude)
    }

    private fun moveCameraOnLocation(latitude: Double, longitude: Double) {
        mMap.moveCamera(CameraUpdateFactory
                .newCameraPosition(CameraPosition
                        .Builder()
                        .target(LatLng(latitude, longitude))
                        .zoom(15f)
                        .build()
                )
        )
    }

    private fun moveOnLastLocation() {
        activity?.let {
            moveCameraOnLocation(mViewModel.location.latitude, mViewModel.location.longitude)
        }
    }
}
