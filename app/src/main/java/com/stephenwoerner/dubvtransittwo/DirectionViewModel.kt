package com.stephenwoerner.dubvtransittwo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.maps.model.LatLng

class DirectionViewModel : ViewModel() {

    private val carDirections = MutableLiveData<ArrayList<String>>()
    private val busDirections = MutableLiveData<ArrayList<String>>()
    private val walkingDirections = MutableLiveData<ArrayList<String>>()
    private val prtDirections = MutableLiveData<ArrayList<String>>()


    private val leavingTimeMillis = MutableLiveData<Long>()


    private val destinationStr = MutableLiveData<String>()
    private val closestPRTA = MutableLiveData<String>()
    private val closestPRTB = MutableLiveData<String>()
    private val useCurrentTime = MutableLiveData<Boolean>()

    private val origin = MutableLiveData<LatLng>()
    private val destination = MutableLiveData<LatLng>()


    init {
        leavingTimeMillis.value = 0L
        useCurrentTime.value = false;
    }




}