package com.banana.appwithgeolocation.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.banana.appwithgeolocation.R
import com.banana.appwithgeolocation.model.entity.Point
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
    private var mSelected: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        buttonAddPosition.setOnClickListener {
            if (checkDistance()) {
                showDialog()
            } else {
                requireActivity().showToast("Рядом уже есть точка")
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap.apply {
            isMyLocationEnabled = true
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isMapToolbarEnabled = false
        }

        mViewModel.points.observe(this, Observer { list ->
            mMap.clear()
            list.forEach { addMarker(it) }
        })
        mViewModel.selectedPoint.observe(this, Observer { selected ->
            mMap.clear()
            mViewModel.points.value?.forEach { point ->
                addMarker(point, point == selected)
            }
        })

        moveOnLastLocation()
    }

    private fun addMarker(point: Point, selected: Boolean = false) {
        mMap.addMarker(
            MarkerOptions()
                    .position(LatLng(point.latitude, point.longitude))
                    .title(point.name)
        ).setIcon(BitmapDescriptorFactory.defaultMarker(
            if (selected) BitmapDescriptorFactory.HUE_BLUE else BitmapDescriptorFactory.HUE_RED
        ))
        if (selected) {
            moveCameraOnPoint(point)
        }
    }

    private fun checkDistance(): Boolean {
        val settings = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val accuracy = settings.getInt("appwithgeolocation.settings.ACCURACY", 100)
        return mViewModel.checkDistance(accuracy)
    }

    @SuppressLint("InflateParams")
    private fun showDialog() {
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_new_point, null)
        val name = dialogLayout.name_edittext.text
        val layout = dialogLayout.input_layout

        dialogLayout.latitude_textview.text = getString(R.string.dialog_new_point_latitude,
            mViewModel.location.latitude)
        dialogLayout.longitude_textview.text = getString(R.string.dialog_new_point_longitude,
            mViewModel.location.longitude)

        val dialog = requireActivity()
                .createSimpleDialog(getString(R.string.dialog_new_point_title),
                                    dialogLayout,
                                    getString(R.string.ok),
                                    getString(R.string.cancel))

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
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
                        dialog.dismiss()
                        GlobalScope.launch(Dispatchers.Main) {
                            moveCameraOnLocation(point.latitude, point.longitude)
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun moveCameraOnPoint(point: Point) {
        moveCameraOnLocation(point.latitude, point.longitude)
        mSelected = true
    }

    private fun moveCameraOnLocation(latitude: Double, longitude: Double) {
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                .target(LatLng(latitude, longitude))
                .zoom(15f).build()))
    }

    private fun moveOnLastLocation() {
        if (activity != null) {
            val location = mViewModel.location
            if (location.latitude != 707.0 && location.longitude != 707.0) {
                if (!mSelected) {
                    moveCameraOnLocation(location.latitude, location.longitude)
                }
            }
        }
    }
}
