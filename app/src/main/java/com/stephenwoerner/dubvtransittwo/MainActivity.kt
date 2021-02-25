package com.stephenwoerner.dubvtransittwo

import android.os.Bundle
import android.view.Window
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

/**
 * The main activity
 * Created by Stephen on 3/23/2017.
 */
class MainActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        //initialize
        super.onCreate(savedInstanceState)

        window.requestFeature(Window.FEATURE_ACTION_BAR)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

//        if (savedInstanceState == null) {
//            val fragment = MapFragment()
//            supportFragmentManager
//                .beginTransaction()
//                .add(R.id.nav_host_fragment, fragment)
//                .commit()
//        }
    }
}