package com.orlinskas.videofacefinder.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import com.orlinskas.videofacefinder.core.BaseViewModel

class MainViewModel @ViewModelInject constructor(

) : BaseViewModel() {

    init {
        addSaveStateHandler()
    }

}