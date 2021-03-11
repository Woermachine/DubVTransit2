package com.stephenwoerner.dubvtransittwo.shared.directions

import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.*
import com.stephenwoerner.dubvtransittwo.shared.KLatLng
import java.time.Instant

actual class KDirectionsApi {
    actual fun newRequest(MAPS_KEY: String, leavingTime: Long, origin: KLatLng, destination: KLatLng): StepsAndDuration {
        val geoContext = GeoApiContext.Builder().apiKey(MAPS_KEY).build()
        val instant = Instant.ofEpochSecond(leavingTime)

        val carResult =
            DirectionsApi.newRequest(geoContext).origin(origin.toLatLng()).destination(destination.toLatLng())
                .departureTime(instant).await()
        return getStepsAndDuration(carResult)
    }

    actual fun newBusRequest(MAPS_KEY: String, leavingTime: Long, origin: KLatLng, destination: KLatLng): StepsAndDuration {
        val kMode = TravelMode.TRANSIT
        val kTransitMode = TransitMode.BUS
        val geoContext = GeoApiContext.Builder().apiKey(MAPS_KEY).build()
        val instant = Instant.ofEpochSecond(leavingTime)

        val carResult =
            DirectionsApi.newRequest(geoContext).origin(origin.toLatLng()).destination(destination.toLatLng())
                .departureTime(instant).mode(kMode).transitMode(kTransitMode)
                .await()
        return getStepsAndDuration(carResult)
    }

    actual fun newWalkingRequest(MAPS_KEY: String, leavingTime: Long, origin: KLatLng, destination: KLatLng): StepsAndDuration {
        val geoContext = GeoApiContext.Builder().apiKey(MAPS_KEY).build()
        val instant = Instant.ofEpochSecond(leavingTime)

        val carResult =
            DirectionsApi.newRequest(geoContext).origin(origin.toLatLng()).destination(destination.toLatLng())
                    .departureTime(instant).mode(TravelMode.WALKING).await()
        return getStepsAndDuration(carResult)
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

private fun KLatLng.toLatLng(): LatLng {
    return LatLng(this.lat, this.lng)
}

private fun Duration.toKDuration(): KDuration {
    return KDuration(this.inSeconds, this.humanReadable)
}

private fun Distance.toKDistance(): KDistance {
    return KDistance(this.inMeters, this.humanReadable)
}