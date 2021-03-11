package com.stephenwoerner.dubvtransittwo

import android.content.Context
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.stephenwoerner.dubvtransittwo.shared.AndroidContext
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import timber.log.Timber.DebugTree


/**
 * The main activity
 * Created by Stephen on 3/23/2017.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        //initialize
        super.onCreate(savedInstanceState)
        AndroidContext.context = applicationContext

        window.requestFeature(Window.FEATURE_ACTION_BAR)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }
}