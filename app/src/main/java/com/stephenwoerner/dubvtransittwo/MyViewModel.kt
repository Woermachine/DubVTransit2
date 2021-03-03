package com.stephenwoerner.dubvtransittwo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.maps.model.LatLng

class MyViewModel : ViewModel() {

    var destination: MutableLiveData<String> = MutableLiveData()
    var source: MutableLiveData<String> = MutableLiveData()

    private val carDirections = MutableLiveData<ArrayList<String>>()
    private val busDirections = MutableLiveData<ArrayList<String>>()
    private val walkingDirections = MutableLiveData<ArrayList<String>>()
    private val prtDirections = MutableLiveData<ArrayList<String>>()


    private val leavingTimeMillis = MutableLiveData<Long>()


    private val destinationStr = MutableLiveData<String>()
    private val closestPRTA = MutableLiveData<String>()
    private val closestPRTB = MutableLiveData<String>()
    private val useCurrentTime = MutableLiveData<Boolean>()

    private val originLatLng = MutableLiveData<LatLng>()
    private val destinationLatLng = MutableLiveData<LatLng>()


    init {
        destination.value = "Destination"
        source.value = "Current Location"
        leavingTimeMillis.value = 0L
        useCurrentTime.value = false
    }

    fun setSource(dest: String) {
        source.value = dest
    }

    fun setDestination(dest: String) {
        destination.value = dest
    }
}