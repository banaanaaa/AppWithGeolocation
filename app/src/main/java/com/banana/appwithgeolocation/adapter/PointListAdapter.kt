package com.banana.appwithgeolocation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.banana.appwithgeolocation.R
import com.banana.appwithgeolocation.databinding.PointItemBinding
import com.banana.appwithgeolocation.model.entity.Point
import com.banana.appwithgeolocation.view.FragmentList

class PointListAdapter(
    private val points: LiveData<List<Point>>,
    private val listener: FragmentList.Listener
) : RecyclerView.Adapter<PointListAdapter.PointViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PointViewHolder(
        DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.point_item, parent, false
        )
    )

    override fun onBindViewHolder(holder: PointViewHolder, position: Int) {
        holder.bind(points.value!![position], listener)
    }

    override fun getItemCount() = points.value?.size ?: 0


    class PointViewHolder(private val binding: PointItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(point: Point, listener: FragmentList.Listener) {
            binding.point = point
            binding.listener = listener
        }
    }
}