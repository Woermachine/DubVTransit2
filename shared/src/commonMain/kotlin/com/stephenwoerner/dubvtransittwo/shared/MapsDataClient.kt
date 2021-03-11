package com.stephenwoerner.dubvtransittwo.shared

import com.soywiz.klock.DateTime
import com.stephenwoerner.dubvtransittwo.shared.directions.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlin.collections.ArrayList

class MapsDataClient(val MAPS_KEY: String) {

    private lateinit var cSAD: StepsAndDuration
    private lateinit var bSAD: StepsAndDuration
    private lateinit var wSAD: StepsAndDuration
    private lateinit var pSAD: StepsAndDuration

    suspend fun execute(
        leavingTimeMillis: Long,
        origin: KLatLng,
        destination: KLatLng
    ): MapsTaskResults {
        val model = PRTModel.get()
        var leavingTime = leavingTimeMillis

        // If leaving time is in the past, set to current time
        val currTime = DateTime.nowUnixLong()
        if (leavingTime < currTime)
            leavingTime = currTime

        val closestPRTA = model.findClosestPRT(origin)
        val closestPRTB = model.findClosestPRT(destination)

        val closestPRTAStr = model.allHashMap[closestPRTA]!!
        val closestPRTBStr = model.allHashMap[closestPRTB]!!

        val deferredCar = GlobalScope.async {
            cSAD = KDirectionsApi().newRequest(MAPS_KEY, leavingTime, origin, destination)

            "car"
        }

        val deferredBus = GlobalScope.async {
            bSAD = KDirectionsApi().newBusRequest(MAPS_KEY, leavingTime, origin, destination)

            "bus"
        }

        val deferredWalk = GlobalScope.async {
            wSAD = KDirectionsApi().newWalkingRequest(MAPS_KEY, leavingTime, origin, destination)

            "walk"
        }

        val deferredPrt = GlobalScope.async {
            model.requestPRTStatus()
            if (model.isOpenNow()) {
                val pSADA = KDirectionsApi().newRequest(MAPS_KEY, leavingTime, origin, closestPRTAStr)
                val pSADB = KDirectionsApi().newRequest(MAPS_KEY, leavingTime, closestPRTBStr, destination)

                val prtDirection = "Ride Prt from $closestPRTA to $closestPRTB"
                val inSeconds =
                    model.estimateTime(closestPRTA, closestPRTB, leavingTime).toLong()
                val humanReadable = "${inSeconds} seconds"
                val prtDuration = KDuration(inSeconds, humanReadable)


                var fullDir = pSADA.duration
                fullDir += prtDuration.inSeconds.toInt()
                fullDir += pSADB.duration

                pSAD = StepsAndDuration(ArrayList(), fullDir, true)

                pSAD.directions.addAll(pSADA.directions)
                pSAD.directions.add(SimpleDirections(prtDirection, null, prtDuration))
                pSAD.directions.addAll(pSADB.directions)
            } else {
                val noPRTAL = arrayListOf<SimpleDirections>()
                val inSeconds = 0L
                val humanReadable = "0 seconds"
                val dur = KDuration(inSeconds, humanReadable)
                noPRTAL.add(SimpleDirections("The PRT is currently closed", null, null))
                pSAD = StepsAndDuration(noPRTAL, 0, false)
            }

            "prt"
        }

        println("${deferredCar.await()},${deferredBus.await()},${deferredWalk.await()},${deferredPrt.await()}")


        var fastest = cSAD.duration
        var fastestRoute = Route.CAR

        if (bSAD.isAvailable && bSAD.duration < fastest) {
            fastest = bSAD.duration
            fastestRoute = Route.BUS
        }
        if (wSAD.isAvailable && wSAD.duration < fastest) {
            fastest = wSAD.duration
            fastestRoute = Route.WALK
        }
        if (pSAD.isAvailable && pSAD.duration < fastest) {
            fastestRoute = Route.PRT
        }

        return MapsTaskResults(
            carStepsAndDuration = cSAD,
            busStepsAndDuration = bSAD,
            walkStepsAndDuration = wSAD,
            prtStepsAndDuration = pSAD,
            fastestRoute = fastestRoute,
            closestPRTA = closestPRTA,
            closestPRTB = closestPRTB,
            leavingTime = leavingTime
        )
    }

}
