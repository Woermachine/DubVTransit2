package com.stephenwoerner.dubvtransittwo

import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.*
import com.soywiz.klock.DateTime
import com.stephenwoerner.dubvtransittwo.shared.KLatLng
import com.stephenwoerner.dubvtransittwo.shared.PRTModel
import com.stephenwoerner.dubvtransittwo.shared.directions.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import timber.log.Timber
import java.time.Instant
import kotlin.collections.ArrayList

class MapsDataClient {

    private lateinit var cSAD: StepsAndDuration
    private lateinit var bSAD: StepsAndDuration
    private lateinit var wSAD: StepsAndDuration
    private lateinit var pSAD: StepsAndDuration

    suspend fun execute(
        leavingTimeMillis: Long,
        origin: LatLng,
        destination: LatLng
    ): MapsTaskResults {
        val model = PRTModel.get()
        var leavingTime = leavingTimeMillis

        // If leaving time is in the past, set to current time
        val currTime = DateTime.nowUnixLong()
        if (leavingTime < currTime)
            leavingTime = currTime

        val closestPRTA = model.findClosestPRT(origin.toKLatLng())
        val closestPRTB = model.findClosestPRT(destination.toKLatLng())

        val closestPRTAStr = model.allHashMap[closestPRTA]!!.toLatLng()
        val closestPRTBStr = model.allHashMap[closestPRTB]!!.toLatLng()

        val geoContext = GeoApiContext.Builder().apiKey(BuildConfig.MAPS_KEY).build()
        val instant = Instant.ofEpochSecond(leavingTime)

        val deferredCar = GlobalScope.async {
            val carResult =
                DirectionsApi.newRequest(geoContext).origin(origin).destination(destination)
                    .departureTime(instant).await()
            cSAD = getStepsAndDuration(carResult)

            "car"
        }

        val deferredBus = GlobalScope.async {
            val busResult =
                DirectionsApi.newRequest(geoContext).origin(origin).destination(destination)
                    .departureTime(instant).mode(TravelMode.TRANSIT).transitMode(TransitMode.BUS)
                    .await()
            bSAD = getStepsAndDuration(busResult)

            "bus"
        }

        val deferredWalk = GlobalScope.async {
            val walkingResult =
                DirectionsApi.newRequest(geoContext).origin(origin).destination(destination)
                    .departureTime(instant).mode(TravelMode.WALKING).await()
            wSAD = getStepsAndDuration(walkingResult)

            "walk"
        }

        val deferredPrt = GlobalScope.async {
            model.requestPRTStatus()
            if (model.isOpenNow()) {
                val prtResultA =
                    DirectionsApi.newRequest(geoContext).origin(origin).destination(closestPRTAStr)
                        .departureTime(instant).mode(TravelMode.WALKING).await()
                val prtResultB = DirectionsApi.newRequest(geoContext).origin(closestPRTBStr)
                    .destination(destination).departureTime(instant).mode(TravelMode.WALKING)
                    .await()
                val pSADA = getStepsAndDuration(prtResultA)
                val pSADB = getStepsAndDuration(prtResultB)

                val prtDirection = "Ride Prt from $closestPRTA to $closestPRTB"
                val prtDuration = Duration()
                prtDuration.inSeconds =
                    model.estimateTime(closestPRTA, closestPRTB, leavingTime).toLong()
                prtDuration.humanReadable = "${prtDuration.inSeconds} seconds"


                var fullDir = pSADA.duration
                fullDir += prtDuration.inSeconds.toInt()
                fullDir += pSADB.duration

                pSAD = StepsAndDuration(ArrayList(), fullDir, true)

                pSAD.directions.addAll(pSADA.directions)
                pSAD.directions.add(SimpleDirections(prtDirection, null, prtDuration.toKDuration()))
                pSAD.directions.addAll(pSADB.directions)
            } else {
                val noPRTAL = arrayListOf<SimpleDirections>()
                val dur = Duration()
                dur.inSeconds = 0
                dur.humanReadable = "0 seconds"
                noPRTAL.add(SimpleDirections("The PRT is currently closed", null, null))
                pSAD = StepsAndDuration(noPRTAL, 0, false)
            }

            "prt"
        }


        Timber.d(
            "%s%s%s%s",
            deferredCar.await(),
            deferredBus.await(),
            deferredWalk.await(),
            deferredPrt.await()
        )


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

    private fun formatInstruct(step: DirectionsStep): String {
        return step.htmlInstructions.replace("<[^>]*>".toRegex(), "").trimIndent()
    }

    private fun getStepsAndDuration(dr: DirectionsResult): StepsAndDuration {
        val directions = arrayListOf<SimpleDirections>()
        var duration = 0
        val isAvailable = dr.routes.isNotEmpty()
        if (isAvailable) {
            dr.routes[0]?.legs?.forEach { leg ->
                leg.steps?.forEach {
                    directions.add(SimpleDirections(formatInstruct(it), it.distance.toKDistance(), it.duration.toKDuration()))
                }
                duration += leg.duration.inSeconds.toInt()
            }
        } else {
            directions.add(SimpleDirections("Currently unavailable", null, null))
        }
        return StepsAndDuration(directions, duration, isAvailable)
    }








}

private fun LatLng.toKLatLng(): KLatLng {
    return KLatLng(this.lat, this.lng)
}

private fun KLatLng.toLatLng(): LatLng {
    return LatLng(this.lat, this.lng)
}

private fun Duration.toKDuration(): KDuration {
    return KDuration(this.inSeconds, this.humanReadable)
}

private fun Distance.toKDistance(): KDistance {
    return KDistance(this.inMeters, this.humanReadable)
}
