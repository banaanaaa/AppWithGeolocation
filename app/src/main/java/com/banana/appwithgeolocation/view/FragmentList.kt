package com.banana.appwithgeolocation.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.isNotEmpty
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.banana.appwithgeolocation.viewmodel.MainViewModel
import com.banana.appwithgeolocation.R
import com.banana.appwithgeolocation.adapter.PointListAdapter
import com.banana.appwithgeolocation.model.entity.Point
import com.banana.appwithgeolocation.utils.createClearLayout
import com.banana.appwithgeolocation.utils.createSimpleDialog
import com.banana.appwithgeolocation.utils.showToast
import kotlinx.android.synthetic.main.dialog_input.view.*
import kotlinx.android.synthetic.main.fragment_list.*

class FragmentList : Fragment() {

    private lateinit var mViewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        val adapter = createPointListAdapter()

        recycler_view?.let {
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(context)
        }

        mViewModel.points.observe(viewLifecycleOwner, Observer { live ->
            live.let { adapter.notifyDataSetChanged() }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_option_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.nav_delete) {
            if (recycler_view.isNotEmpty()) showDialog()
            else requireContext().showToast(getString(R.string.toast_list_is_empty), Toast.LENGTH_LONG)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createPointListAdapter() = PointListAdapter(
        mViewModel.points, object : Listener {
            override fun click(point: Point) {
                mViewModel.selectMarker(point.name)
                findNavController().navigate(R.id.destination_map)
            }

            override fun rename(point: Point) { showDialog(point) }

            override fun delete(point: Point) { showDialog(deleteAll = false, point = point) }
        }
    )

    private fun showDialog(deleteAll: Boolean = true, point: Point? = null) {
        requireContext().createSimpleDialog(
            getString(R.string.dialog_delete_title),
            requireContext().createClearLayout(
                    if (deleteAll) getString(R.string.dialog_delete_all_desc)
                    else getString(R.string.dialog_delete_one_desc)),
            getString(R.string.dialog_button_positive_yes),
            getString(R.string.dialog_button_negative_cancel)
        ).apply {
            setOnShowListener {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    if (deleteAll) mViewModel.deletePoints()
                    else point?.let { mViewModel.deletePoint(it) }
                    dismiss()
                }
            }
            show()
        }
    }

    @SuppressLint("InflateParams")
    private fun showDialog(point: Point) {
        val layout = layoutInflater.inflate(R.layout.dialog_rename_point, null)
        val inputName = layout.text_input_edit_text.text
        val inputLayout = layout.text_input_layout

        requireContext().createSimpleDialog(
            getString(R.string.dialog_rename_title),
            layout,
            getString(R.string.dialog_button_positive_rename),
            getString(R.string.dialog_button_negative_cancel)
        ).apply {
            setOnShowListener {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    inputLayout.error = when (mViewModel.renamePoint(point, inputName.toString())) {
                        MainViewModel.NameValidationResult.TOO_SHORT ->
                            getString(R.string.error_name_is_short)
                        MainViewModel.NameValidationResult.TOO_LONG ->
                            getString(R.string.error_name_is_long)
                        MainViewModel.NameValidationResult.ALREADY_EXISTS ->
                            getString(R.string.error_name_is_taken)
                        MainViewModel.NameValidationResult.SUCCESS -> {
                            dismiss()
                            ""
                        }
                    }
                }
            }
            show()
        }
    }


    interface Listener {
        fun click(point: Point)
        fun rename(point: Point)
        fun delete(point: Point)
    }

}
