package com.stephenwoerner.dubvtransittwo

import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity

/**
 * The main activity
 * Created by Stephen on 3/23/2017.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)
    }
}