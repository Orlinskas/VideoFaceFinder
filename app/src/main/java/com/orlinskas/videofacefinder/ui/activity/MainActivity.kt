package com.orlinskas.videofacefinder.ui.activity

import android.os.Bundle
import com.example.videofacefinder.R
import com.example.videofacefinder.databinding.ActivityMainBinding
import com.orlinskas.videofacefinder.core.BaseActivity
import com.orlinskas.videofacefinder.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = getViewModel()
        binding = bindContentView(R.layout.activity_main)
        // or
        //setContentView(R.layout.activity_main)

    }
}