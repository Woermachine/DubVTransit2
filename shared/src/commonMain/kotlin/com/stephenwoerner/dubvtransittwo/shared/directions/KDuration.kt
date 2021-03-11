package com.stephenwoerner.dubvtransittwo.shared.directions

class KDuration(val inSeconds: Long, val humanReadable: String) {
    override fun toString(): String {
        return humanReadable
    }
}