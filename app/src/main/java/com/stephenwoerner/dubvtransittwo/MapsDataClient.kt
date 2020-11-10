package com.stephenwoerner.dubvtransittwo

import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.*
import com.soywiz.klock.DateTime
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import timber.log.Timber
import java.time.Instant
import kotlin.collections.ArrayList

class MapsDataClient {

    private lateinit var cSAD : StepsAndDuration
    private lateinit var bSAD : StepsAndDuration
    private lateinit var wSAD : StepsAndDuration
    private lateinit var pSAD : StepsAndDuration

    suspend fun execute(leavingTimeMillis : Long, origin : LatLng, destination : LatLng) : MapsTaskResults {
        val model = PRTModel.get()
        var leavingTime = leavingTimeMillis

        // If leaving time is in the past, set to current time
        val currTime = DateTime.nowUnixLong()
        if (leavingTime < currTime)
            leavingTime = currTime

        val closestPRTA = model.findClosestPRT(origin)
        val closestPRTB = model.findClosestPRT(destination)

        val closestPRTAStr = model.allHashMap[closestPRTA]
        val closestPRTBStr = model.allHashMap[closestPRTB]

        val geoContext = GeoApiContext.Builder().apiKey(BuildConfig.MAPS_KEY).build()
        val instant = Instant.ofEpochSecond(leavingTime)

        val deferredCar = GlobalScope.async {
            val carResult = DirectionsApi.newRequest(geoContext).origin(origin).destination(destination).departureTime(instant).await()
            cSAD = getStepsAndDuration(carResult)

            "car"
        }

        val deferredBus = GlobalScope.async {
            val busResult = DirectionsApi.newRequest(geoContext).origin(origin).destination(destination).departureTime(instant).mode(TravelMode.TRANSIT).transitMode(TransitMode.BUS).await()
            bSAD = getStepsAndDuration(busResult)

           "bus"
        }

        val deferredWalk = GlobalScope.async {
            val walkingResult = DirectionsApi.newRequest(geoContext).origin(origin).destination(destination).departureTime(instant).mode(TravelMode.WALKING).await()
            wSAD = getStepsAndDuration(walkingResult)
            "prt"
        }

       val deferredPrt = GlobalScope.async {
            val prtResultA = DirectionsApi.newRequest(geoContext).origin(origin).destination(closestPRTAStr).departureTime(instant).mode(TravelMode.WALKING).await()
            val prtResultB = DirectionsApi.newRequest(geoContext).origin(closestPRTBStr).destination(destination).departureTime(instant).mode(TravelMode.WALKING).await()

            val pSADA = getStepsAndDuration(prtResultA)
            val pSADB = getStepsAndDuration(prtResultB)


            var prtDuration = pSADA.duration
            prtDuration += model.estimateTime(closestPRTA, closestPRTB, leavingTime).toInt()
            prtDuration += pSADB.duration

            pSAD = StepsAndDuration(ArrayList(), prtDuration)

            pSAD.directions.addAll(pSADA.directions)
            pSAD.directions.add("Ride Prt from $closestPRTA to $closestPRTB")
            pSAD.directions.addAll(pSADB.directions)
            "prt"
        }


        Timber.d("%s%s%s%s", deferredCar.await(), deferredBus.await(), deferredWalk.await(), deferredPrt.await())


        var fastest = cSAD.duration
        var fastestRoute = DirectionActivity.Route.CAR

        if (bSAD.duration < fastest) {
            fastest = bSAD.duration
            fastestRoute = DirectionActivity.Route.BUS
        }
        if (wSAD.duration < fastest) {
            fastest = wSAD.duration
            fastestRoute = DirectionActivity.Route.WALK
        }
        if (pSAD.duration < fastest) {
            fastestRoute = DirectionActivity.Route.PRT
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

    private fun formatInstruct(step : DirectionsStep) : String {
        return  """${step.htmlInstructions.replace("<[^>]*>".toRegex(), "")}${step.distance}""".trimIndent()
    }

    private fun getStepsAndDuration(dr : DirectionsResult) : StepsAndDuration {
        val directions = arrayListOf<String>()
        var duration = 0
        if(dr.routes.isEmpty()) {
            directions.add("No routes found")
            duration = Int.MAX_VALUE
        } else {
            dr.routes[0].legs.forEach { leg ->
                leg.steps.forEach {
                    directions.add(formatInstruct(it))
                }
                duration += leg.duration.inSeconds.toInt()
            }
        }
        return StepsAndDuration(directions, duration)
    }

    data class StepsAndDuration(val directions : ArrayList<String>, val duration: Int)

    data class MapsTaskResults(val carStepsAndDuration: StepsAndDuration, val busStepsAndDuration: StepsAndDuration,
                               val walkStepsAndDuration: StepsAndDuration, val prtStepsAndDuration: StepsAndDuration,
                               val fastestRoute : DirectionActivity.Route, val closestPRTA : String,
                               val closestPRTB : String, val leavingTime : Long)

}