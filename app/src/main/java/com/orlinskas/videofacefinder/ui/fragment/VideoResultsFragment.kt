package com.orlinskas.videofacefinder.ui.fragment

import android.os.Bundle
import android.view.View
import com.example.videofacefinder.R
import com.example.videofacefinder.databinding.FragmentVideoResultsBinding
import com.orlinskas.videofacefinder.core.BaseFragment
import com.orlinskas.videofacefinder.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoResultsFragment : BaseFragment() {

    override val layoutResId: Int
        get() = R.layout.fragment_video_results

    private val binding: FragmentVideoResultsBinding
        get() = fragmentDataBinding as FragmentVideoResultsBinding

    private val viewModel: MainViewModel by lazy {
        obtainViewModel(requireActivity(), MainViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}