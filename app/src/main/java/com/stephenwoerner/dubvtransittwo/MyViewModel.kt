package com.stephenwoerner.dubvtransittwo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MyViewModel : ViewModel() {
    lateinit var destination: MutableLiveData<String>
    lateinit var source: MutableLiveData<String>

    init {
        destination.value = "Un"
        source.value = "Current Location"
    }

    fun getSource(): String {
        return source.value!!
    }

    fun setSource(dest : String) {
        source.value = dest
    }

    fun getDestination(): String {
        return source.value!!
    }

    fun setDestination(dest : String) {
        source.value = dest
    }
}