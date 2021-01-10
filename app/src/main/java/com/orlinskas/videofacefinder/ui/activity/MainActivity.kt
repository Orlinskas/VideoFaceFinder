package com.orlinskas.videofacefinder.ui.activity

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.example.videofacefinder.R
import com.example.videofacefinder.databinding.ActivityMainBinding
import com.orlinskas.videofacefinder.core.BaseActivity
import com.orlinskas.videofacefinder.extensions.singleObserve
import com.orlinskas.videofacefinder.service.VideoProcessService
import com.orlinskas.videofacefinder.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_main)
        viewModel = getViewModel()
        navController = findNavController(R.id.navHostFragment)

        val navOptions = NavOptions.Builder()
        navOptions.apply {
            setEnterAnim(R.anim.enter_from_right)
            setExitAnim(R.anim.exit_to_left)
            setPopEnterAnim(R.anim.enter_from_left)
            setPopExitAnim(R.anim.exit_to_right)
        }

        binding.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.video -> {
                    if (navController.currentDestination?.id != R.id.videoProcessFragment) {
                        navController.navigate(R.id.videoProcessFragment, null, navOptions.build())
                    }
                    true
                }
                R.id.results -> {
                    if (navController.currentDestination?.id != R.id.videoResultsFragment) {
                        navController.navigate(R.id.videoResultsFragment, null, navOptions.build())
                    }
                    true
                }
                R.id.persons -> {
                    if (navController.currentDestination?.id != R.id.personsListFragment) {
                        navController.navigate(R.id.personsListFragment, null, navOptions.build())
                    }
                    true
                }
                else -> false
            }
        }

        viewModel.videoProcessLiveData.singleObserve(this) { state ->
            if (state == VideoProcessService.State.SUCCESS) {
                binding.bottomNavigation.selectedItemId = R.id.results

                when(navController.currentDestination?.id) {
                    R.id.videoProcessFragment -> {
                        navController.navigate(R.id.videoResultsFragment, null, navOptions.build())
                    }
                    R.id.personsListFragment -> {
                        navController.navigate(R.id.videoResultsFragment, null, navOptions.build())
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (!viewModel.state.isProgress) {
            super.onBackPressed()
            this.finish()
        }
    }
}