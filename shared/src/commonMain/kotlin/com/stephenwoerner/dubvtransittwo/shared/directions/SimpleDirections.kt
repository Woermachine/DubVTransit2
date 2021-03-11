package com.stephenwoerner.dubvtransittwo.shared.directions

data class SimpleDirections(
    val direction: String,
    val stepDistance: KDistance?,
    val stepDuration: KDuration?
)