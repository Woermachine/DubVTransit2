package com.stephenwoerner.dubvtransittwo

import android.app.Activity
import android.content.res.Configuration
import android.view.View

class AppUtils {
    companion object {
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