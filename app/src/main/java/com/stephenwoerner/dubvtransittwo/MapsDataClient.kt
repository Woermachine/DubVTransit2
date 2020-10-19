package com.stephenwoerner.dubvtransittwo

import android.content.Context
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.joda.time.Instant
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

class MapsDataClient {

    private lateinit var context : Context

    private lateinit var origin : LatLng
    private lateinit var destination : LatLng

    private var leavingTime = Calendar.getInstance()

    private lateinit var closestPRTA: String
    private lateinit var closestPRTB: String

    private var fastestRoute = DirectionActivity.Route.CAR

    private var useCurrentTime = true


    private lateinit var cSAD : StepsAndDuration
    private lateinit var bSAD : StepsAndDuration
    private lateinit var wSAD : StepsAndDuration
    private lateinit var pSAD : StepsAndDuration

    suspend fun execute(vararg params: Any?) : MapsTaskResults{
        leavingTime.timeInMillis = params[0] as Long
        origin = params[1] as LatLng
        destination = params[2] as LatLng
        useCurrentTime = params[3] as Boolean
        context = params[4] as Context
        val model = PRTModel.get(context)

        // If leaving time is in the past, set to current time
        val currTime = Calendar.getInstance()
        if (leavingTime.timeInMillis < currTime.timeInMillis)
            leavingTime.timeInMillis = currTime.timeInMillis

        closestPRTA = model.findClosestPRT(origin)
        closestPRTB = model.findClosestPRT(destination)

        val closestPRTAStr = model.allHashMap[closestPRTA]
        val closestPRTBStr = model.allHashMap[closestPRTB]

        val geoContext = GeoApiContext().setApiKey(BuildConfig.MAPS_KEY)
        val instant = Instant(leavingTime.timeInMillis)

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
        fastestRoute = DirectionActivity.Route.CAR

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
                               val closestPRTB : String, val leavingTime : Calendar)

}