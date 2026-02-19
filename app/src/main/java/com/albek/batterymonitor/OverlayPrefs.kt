package com.albek.batterymonitor

import android.content.Context
import androidx.core.content.edit

object OverlayPrefs {
    private const val PREFS_NAME = "overlay_prefs"

    const val KEY_SHOW_VOLTAGE = "show_voltage"
    const val KEY_SHOW_CURRENT = "show_current"
    const val KEY_SHOW_TEMPERATURE = "show_temperature"
    const val KEY_SHOW_LEVEL = "show_level"

    fun getBoolean(context: Context, key: String, defaultValue: Boolean): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(key, defaultValue)
    }

    fun setBoolean(context: Context, key: String, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(key, value)
        }
    }
}
