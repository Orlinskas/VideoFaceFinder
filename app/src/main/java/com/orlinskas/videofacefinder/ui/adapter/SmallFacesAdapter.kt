package com.orlinskas.videofacefinder.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.videofacefinder.R
import com.example.videofacefinder.databinding.ItemSmallFaceBinding
import com.orlinskas.videofacefinder.data.model.FaceModel
import com.orlinskas.videofacefinder.extensions.bindWith

class SmallFacesAdapter : RecyclerView.Adapter<SmallFacesAdapter.Holder>() {

    private val _data = mutableListOf<FaceModel>()

    var onFaceClick: ((FaceModel) -> (Unit))? = null

    var data: List<FaceModel>
        set(value) {
            _data.clear()
            _data.addAll(value)
            notifyDataSetChanged()
        }
        get() = _data


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(parent.bindWith(R.layout.item_small_face))
    }

    override fun getItemCount() = _data.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val face = _data[position]
        holder.binding.face = face
        holder.binding.root.setOnClickListener {
            onFaceClick?.invoke(face)
        }
    }

    class Holder(val binding: ItemSmallFaceBinding) : RecyclerView.ViewHolder(binding.root) {}
}