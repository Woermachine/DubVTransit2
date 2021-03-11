package com.stephenwoerner.dubvtransittwo.shared.directions

data class KDistance(val inMeters: Long, val humanReadable: String) {
    override fun toString(): String {
        return humanReadable
    }
}