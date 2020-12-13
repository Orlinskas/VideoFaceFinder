package com.orlinskas.videofacefinder.ui.fragment

import com.orlinskas.videofacefinder.core.BaseFragment
import com.orlinskas.videofacefinder.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment : BaseFragment() {

    override val layoutResId: Int
        get() = TODO("Not yet implemented")

    private val viewModel = getViewModel<MainViewModel>()

    // or

    private val viewModelObtained: MainViewModel by lazy {
        obtainViewModel(requireActivity(), MainViewModel::class.java)
    }

}