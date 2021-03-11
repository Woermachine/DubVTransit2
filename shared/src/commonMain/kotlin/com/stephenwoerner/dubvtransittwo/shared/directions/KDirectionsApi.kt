package com.stephenwoerner.dubvtransittwo.shared.directions

import com.stephenwoerner.dubvtransittwo.shared.KLatLng

expect class KDirectionsApi() {
    fun newRequest(MAPS_KEY: String, leavingTime: Long, origin: KLatLng, destination: KLatLng): StepsAndDuration
    fun newBusRequest(MAPS_KEY: String, leavingTime: Long, origin: KLatLng, destination: KLatLng): StepsAndDuration
    fun newWalkingRequest(MAPS_KEY: String, leavingTime: Long, origin: KLatLng, destination: KLatLng): StepsAndDuration
}