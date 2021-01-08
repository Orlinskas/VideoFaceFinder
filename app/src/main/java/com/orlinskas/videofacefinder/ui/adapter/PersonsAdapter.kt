package com.orlinskas.videofacefinder.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.videofacefinder.R
import com.example.videofacefinder.databinding.ItemPersonBinding
import com.orlinskas.videofacefinder.data.model.FaceModel
import com.orlinskas.videofacefinder.data.model.Person
import com.orlinskas.videofacefinder.extensions.bindWith

class PersonAdapter : RecyclerView.Adapter<PersonAdapter.Holder>() {

    private val _data = mutableListOf<Pair<Person, List<FaceModel>>>()
    private val expandedViewsSet = mutableSetOf<Int>()

    var onPersonClick: ((Person) -> (Unit))? = null

    var data: List<Pair<Person, List<FaceModel>>>
        set(value) {
            _data.clear()
            _data.addAll(value)
            notifyDataSetChanged()
        }
        get() = _data

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(parent.bindWith(R.layout.item_person))
    }

    override fun getItemCount() = _data.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val person = _data[position].first
        val faces = _data[position].second

        holder.binding.person = person
        holder.adapter.data = faces

        holder.binding.root.setOnClickListener { expandView(holder.binding, position) }
        holder.binding.imageView.setOnClickListener { onPersonClick?.invoke(person) }

        val value = expandedViewsSet.find { it == position }

        if (value == null) {
            //holder.binding.imageExpand.rotation = 0f
            holder.binding.recyclerView.visibility = View.GONE
        } else {
            //holder.binding.imageExpand.rotation = 180f
            holder.binding.recyclerView.visibility = View.VISIBLE
        }

        //holder.adapter.onItemClick = onPersonClick
    }

    private fun expandView(binding: ItemPersonBinding, position: Int) {
        val value = expandedViewsSet.find { it == position }

        if (value == null) {
            //binding.imageExpand.rotation = 180f
            binding.recyclerView.visibility = View.VISIBLE
            expandedViewsSet.add(position)
        } else {
            //binding.imageExpand.rotation = 0f
            binding.recyclerView.visibility = View.GONE
            expandedViewsSet.remove(position)
        }
    }

    class Holder(val binding: ItemPersonBinding) : RecyclerView.ViewHolder(binding.root) {

        val adapter = SmallFacesAdapter()

        init {
            binding.recyclerView.layoutManager = LinearLayoutManager(binding.root.context).apply {
                orientation = LinearLayoutManager.HORIZONTAL
            }
            binding.recyclerView.adapter = adapter
        }
    }
}