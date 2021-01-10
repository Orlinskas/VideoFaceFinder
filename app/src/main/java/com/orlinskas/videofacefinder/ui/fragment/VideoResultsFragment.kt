package com.orlinskas.videofacefinder.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.videofacefinder.R
import com.example.videofacefinder.databinding.FragmentVideoResultsBinding
import com.orlinskas.videofacefinder.core.BaseFragment
import com.orlinskas.videofacefinder.extensions.singleObserve
import com.orlinskas.videofacefinder.service.VideoProcessService
import com.orlinskas.videofacefinder.ui.adapter.PersonAdapter
import com.orlinskas.videofacefinder.ui.adapter.SmallFacesAdapter
import com.orlinskas.videofacefinder.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoResultsFragment : BaseFragment() {

    override val layoutResId: Int
        get() = R.layout.fragment_video_results

    private val binding: FragmentVideoResultsBinding
        get() = fragmentDataBinding as FragmentVideoResultsBinding

    private lateinit var adapter: PersonAdapter

    private val viewModel: MainViewModel by lazy {
        obtainViewModel(requireActivity(), MainViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PersonAdapter()

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.getLastPersons().singleObserve(viewLifecycleOwner) {
            adapter.data = it
        }

        viewModel.videoProcessLiveData.singleObserve(this) { state ->
            if (state == VideoProcessService.State.SUCCESS) {
                adapter.notifyDataSetChanged()
            }
        }
    }
}