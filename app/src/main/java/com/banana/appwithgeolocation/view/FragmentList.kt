package com.banana.appwithgeolocation.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.banana.appwithgeolocation.viewmodel.MainViewModel
import com.banana.appwithgeolocation.R
import com.banana.appwithgeolocation.adapter.PointListAdapter
import com.banana.appwithgeolocation.model.entity.Point
import com.banana.appwithgeolocation.utils.createSimpleDialog
import com.banana.appwithgeolocation.utils.showToast
import kotlinx.android.synthetic.main.dialog_input.view.*
import kotlinx.android.synthetic.main.fragment_list.*

class FragmentList : Fragment() {

    private lateinit var mViewModel: MainViewModel

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

        val adapter = createPointListAdapter()
        recyclerview.adapter = adapter
        recyclerview.layoutManager = LinearLayoutManager(context)

        mViewModel.points.observe(viewLifecycleOwner, Observer { live ->
            live.let { adapter.notifyDataSetChanged() }
        })
    }

    private fun createPointListAdapter() = PointListAdapter(
        mViewModel.points, object : Listener {
            override fun click(point: Point) {
                mViewModel.selectMarker(point)
                findNavController().navigate(R.id.map_dest)
            }

            override fun rename(point: Point) {
                showDialog(point)
            }

            override fun delete(point: Point) {
                val dialog = showSureDialog()
                dialog.setMessage(getString(R.string.delete_point))
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        mViewModel.deletePoint(point)
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
        }
    )

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.list_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.nav_delete) {
            val dialog = showSureDialog()
            dialog.setMessage(getString(R.string.delete_points))
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    mViewModel.deletePoints()
                    dialog.dismiss()
                }
            }
            dialog.show()
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("InflateParams")
    fun showSureDialog(): AlertDialog {
        val layout = LinearLayout(requireContext())
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.CENTER
        layout.layoutParams = layoutParams

        return requireActivity()
            .createSimpleDialog(getString(R.string.are_u_sure),
                                layout,
                                getString(R.string.yes),
                                getString(R.string.no))
    }

    @SuppressLint("InflateParams")
    fun showDialog(point: Point) {
        val dialogLayout = layoutInflater.inflate(R.layout.dialog_rename_point, null)
        val name = dialogLayout.name_edittext.text
        val layout = dialogLayout.input_layout

        val dialog = requireActivity()
                .createSimpleDialog(getString(R.string.dialog_rename_point_tittle),
                                    dialogLayout,
                                    getString(R.string.ok),
                                    getString(R.string.cancel))

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                when (mViewModel.renamePoint(point, name.toString())) {
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
                    }
                }
            }
        }

        dialog.show()
    }


    interface Listener {
        fun click(point: Point)
        fun rename(point: Point)
        fun delete(point: Point)
    }
}
