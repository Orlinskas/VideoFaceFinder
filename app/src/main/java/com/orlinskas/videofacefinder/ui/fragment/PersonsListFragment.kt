package com.orlinskas.videofacefinder.ui.fragment

import android.os.Bundle
import android.view.View
import com.example.videofacefinder.R
import com.example.videofacefinder.databinding.FragmentPersonsListBinding
import com.orlinskas.videofacefinder.core.BaseFragment
import com.orlinskas.videofacefinder.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PersonsListFragment : BaseFragment() {

    override val layoutResId: Int
        get() = R.layout.fragment_persons_list

    private val binding: FragmentPersonsListBinding
        get() = fragmentDataBinding as FragmentPersonsListBinding

    private val viewModel: MainViewModel by lazy {
        obtainViewModel(requireActivity(), MainViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

}