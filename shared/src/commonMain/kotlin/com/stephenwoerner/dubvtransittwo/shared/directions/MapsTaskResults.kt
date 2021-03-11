package com.stephenwoerner.dubvtransittwo.shared.directions

data class MapsTaskResults(
    val carStepsAndDuration: StepsAndDuration, val busStepsAndDuration: StepsAndDuration,
    val walkStepsAndDuration: StepsAndDuration, val prtStepsAndDuration: StepsAndDuration,
    val fastestRoute: Route, val closestPRTA: String,
    val closestPRTB: String, val leavingTime: Long
)