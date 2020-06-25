package com.banana.appwithgeolocation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.banana.appwithgeolocation.R
import com.banana.appwithgeolocation.databinding.PointItemBinding
import com.banana.appwithgeolocation.model.entity.Point
import com.banana.appwithgeolocation.view.FragmentList

class PointListAdapter(private val listener: FragmentList.Listener) :
    RecyclerView.Adapter<PointListAdapter.PointViewHolder>() {

    private val points = mutableListOf<Point>()

    internal fun setPoints(points: List<Point>) {
        this.points.clear()
        this.points.addAll(0, points)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PointViewHolder {
        return PointViewHolder(DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.point_item, parent, false))
    }

    override fun onBindViewHolder(holder: PointViewHolder, position: Int) {
        holder.binding.point = points[position]
        holder.binding.listener = listener
    }

    override fun getItemCount() = points.size


    inner class PointViewHolder(val binding: PointItemBinding)
            : RecyclerView.ViewHolder(binding.root)
}