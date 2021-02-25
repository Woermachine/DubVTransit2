package com.stephenwoerner.dubvtransittwo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MyViewModel : ViewModel() {
    var destination: MutableLiveData<String> = MutableLiveData()
    var source: MutableLiveData<String> = MutableLiveData()

    init {
        destination.value = "Destination"
        source.value = "Current Location"
    }

    fun setSource(dest : String) {
        source.value = dest
    }

    fun setDestination(dest : String) {
        destination.value = dest
    }
}