package com.stephenwoerner.dubvtransittwo

import android.app.Activity
import android.content.res.Configuration

class AppUtils {
    companion object {
        @JvmStatic
        fun isDarkTheme(activity: Activity): Boolean {
            return activity.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
    }
}