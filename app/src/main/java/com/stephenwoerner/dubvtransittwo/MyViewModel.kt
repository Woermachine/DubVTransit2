package com.stephenwoerner.dubvtransittwo

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyViewModel(application: Application) : AndroidViewModel(application) {

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

    val prtModel = MutableLiveData<PRTModel>()

    val prtUpdated = Transformations.map(prtModel) {
        prtModel.value?.isOpenNow()
    }


    val test = Transformations.map(prtModel) {
        prtModel.value?.test
    }

    init {
        destination.value = "Destination"
        source.value = "Current Location"
        leavingTimeMillis.value = 0L
        useCurrentTime.value = false
        prtModel.value = PRTModel.get()
    }

    fun setSource(dest: String) {
        source.value = dest
    }

    fun setDestination(dest: String) {
        destination.value = dest
    }

    fun requestPRTStatus() {
        prtModel.value?.test()
        CoroutineScope(Dispatchers.IO).launch {
            prtModel.value?.requestPRTStatus()
        }
    }
}