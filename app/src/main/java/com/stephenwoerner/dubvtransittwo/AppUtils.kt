package com.stephenwoerner.dubvtransittwo

import android.app.Activity
import android.content.res.Configuration
import android.view.View
import java.text.SimpleDateFormat
import java.util.*

class AppUtils {
    companion object {

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)


        @JvmStatic
        fun isDarkTheme(activity: Activity): Boolean {
            return activity.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }

        fun showView(view : View, show: Boolean) {
            view.visibility = if(show) View.VISIBLE else View.GONE
        }
    }
}