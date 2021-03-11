package com.stephenwoerner.dubvtransittwo.shared.directions

data class StepsAndDuration(
    val directions: ArrayList<SimpleDirections>,
    val duration: Int,
    val isAvailable: Boolean
)