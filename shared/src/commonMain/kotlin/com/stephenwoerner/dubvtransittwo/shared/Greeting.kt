package com.stephenwoerner.dubvtransittwo.shared

class Greeting {
    fun greeting(): String {
        return "Hello, ${Platform().platform}!"
    }
}