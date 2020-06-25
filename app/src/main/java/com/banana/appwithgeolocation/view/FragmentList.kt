package com.banana.appwithgeolocation.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.banana.appwithgeolocation.viewmodel.MainViewModel
import com.banana.appwithgeolocation.R
import com.banana.appwithgeolocation.adapter.PointListAdapter
import com.banana.appwithgeolocation.model.entity.Point
import com.banana.appwithgeolocation.util.Navigation
import com.banana.appwithgeolocation.util.Utility
import kotlinx.android.synthetic.main.dialog_input.view.*
import kotlinx.android.synthetic.main.fragment_list.*

class FragmentList : Fragment() {

    private lateinit var mViewModel: MainViewModel
    private var mSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        retainInstance = true
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        val adapter =
            PointListAdapter(object :
                Listener {
                override fun click(point: Point) {
                    mViewModel.selectMarker(point)
                    mSelected = true
                    Navigation.getNavigation().showFragmentById(R.id.map_dest)
                    Navigation.getNavigation().addToBackStack(R.id.map_dest)
                }

                override fun rename(point: Point) {
                    showDialog(point)
                }

                override fun delete(point: Point) {
                    mViewModel.deletePoint(point)
                    mViewModel.deleteMarker(point)
                }
            })

        mViewModel.getPoints().observe(viewLifecycleOwner, Observer { live ->
            live.let { list -> adapter.setPoints(list) }
        })

        recyclerview.adapter = adapter
        recyclerview.layoutManager = LinearLayoutManager(context)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.list_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.nav_delete) {
            mViewModel.deletePoints()
            mViewModel.deleteMarkers()
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("InflateParams")
    fun showDialog(point: Point) {
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_rename_point, null)
        val name = dialogLayout.name_edittext.text
        val layout = dialogLayout.input_layout

        val dialog = Utility.createDialog(
            requireActivity(),
            getString(R.string.dialog_rename_point_tittle), dialogLayout
        )

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                when (mViewModel.renamePoint(point, name.toString())) {
                    MainViewModel.NameValidationResult.TOO_SHORT -> {
                        layout.error = getString(R.string.error_name_is_short)
                        Utility.showToast(requireActivity(), getString(R.string.toast_is_short))
                    }
                    MainViewModel.NameValidationResult.TOO_LONG -> {
                        layout.error = getString(R.string.error_name_is_long)
                        Utility.showToast(requireActivity(), getString(R.string.toast_is_long))
                    }
                    MainViewModel.NameValidationResult.ALREADY_EXISTS -> {
                        // будет скоро
                    }
                    MainViewModel.NameValidationResult.SUCCESS -> {
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    override fun onStart() {
        if (mSelected) {
            mViewModel.returnIcon()
        }
        super.onStart()
    }


    interface Listener {
        fun click(point: Point)
        fun rename(point: Point)
        fun delete(point: Point)
    }
}
