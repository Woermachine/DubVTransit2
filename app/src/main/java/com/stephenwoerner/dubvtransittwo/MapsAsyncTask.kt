package com.stephenwoerner.dubvtransittwo

import android.content.Context
import android.os.AsyncTask
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.*
import org.joda.time.Instant
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

class MapsAsyncTask : AsyncTask<Any, Void?, Boolean?>() {

    interface DAListener {
        fun onResults(mapsTaskResults: MapsTaskResults)
    }

    private lateinit var listener : DAListener
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

    override fun doInBackground(vararg params: Any?): Boolean? {
        leavingTime.timeInMillis = params[0] as Long
        origin = params[1] as LatLng
        destination = params[2] as LatLng
        useCurrentTime = params[3] as Boolean
        context = params[4] as Context
        listener = params[5] as DAListener
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

        //Get Google Maps Travel Times
        val carRequest = DirectionsApi.newRequest(geoContext).origin(origin).destination(destination).departureTime(instant)
        val busRequest = DirectionsApi.newRequest(geoContext).origin(origin).destination(destination).departureTime(instant).mode(TravelMode.TRANSIT).transitMode(TransitMode.BUS)
        val walkingRequest = DirectionsApi.newRequest(geoContext).origin(origin).destination(destination).departureTime(instant).mode(TravelMode.WALKING)
        val prtRequestA = DirectionsApi.newRequest(geoContext).origin(origin).destination(closestPRTAStr).departureTime(instant).mode(TravelMode.WALKING)
        val prtRequestB = DirectionsApi.newRequest(geoContext).origin(closestPRTBStr).destination(destination).departureTime(instant).mode(TravelMode.WALKING)

        try {
            val carResult = carRequest.await()
            val busResult = busRequest.await()
            val walkingResult = walkingRequest.await()
            val prtResultA = prtRequestA.await()
            val prtResultB = prtRequestB.await()

            cSAD = getStepsAndDuration(carResult)
            bSAD = getStepsAndDuration(busResult)
            wSAD = getStepsAndDuration(walkingResult)

            val pSADA = getStepsAndDuration(prtResultA)
            val pSADB = getStepsAndDuration(prtResultB)

            var prtDuration = pSADA.duration
            prtDuration += model.estimateTime(closestPRTA, closestPRTB, leavingTime).toInt()
            prtDuration += pSADB.duration

            pSAD = StepsAndDuration(ArrayList(), prtDuration)

            pSAD.directions.addAll(pSADA.directions)
            pSAD.directions.add("Ride Prt from $closestPRTA to $closestPRTB")
            pSAD.directions.addAll(pSADB.directions)

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
            if (prtDuration < fastest) {
                fastestRoute = DirectionActivity.Route.PRT
            }

        } catch (e: Exception) {
            Timber.e(e)
        }


        return true
    }

    override fun onPostExecute(result: Boolean?) {
        val mapsTaskResults = MapsTaskResults(carStepsAndDuration = cSAD, busStepsAndDuration = bSAD,
                walkStepsAndDuration = wSAD, prtStepsAndDuration = pSAD, fastestRoute = fastestRoute,
                closestPRTA = closestPRTA, closestPRTB = closestPRTB, leavingTime = leavingTime)
        listener.onResults(mapsTaskResults)
    }

    override fun onProgressUpdate(vararg values: Void?) {}

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