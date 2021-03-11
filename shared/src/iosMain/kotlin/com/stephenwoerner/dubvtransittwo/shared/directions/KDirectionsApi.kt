package com.stephenwoerner.dubvtransittwo.shared.directions

actual class KDirectionsApi {
    actual fun newRequest(): StepsAndDuration {
        return StepsAndDuration(arrayListOf<SimpleDirections>(), 0,false)
    }
}