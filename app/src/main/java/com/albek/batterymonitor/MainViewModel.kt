package com.albek.batterymonitor

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.core.content.ContextCompat

class MainViewModel(application: Application) : AndroidViewModel(application) {

    fun startOverlayService() {
        val intent = Intent(getApplication(), BatteryOverlayService::class.java)
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    fun stopOverlayService() {
        val intent = Intent(getApplication(), BatteryOverlayService::class.java)
        getApplication<Application>().stopService(intent)
    }

    fun getShowVoltage(): Boolean =
        OverlayPrefs.getBoolean(getApplication(), OverlayPrefs.KEY_SHOW_VOLTAGE, true)

    fun setShowVoltage(value: Boolean) =
        OverlayPrefs.setBoolean(getApplication(), OverlayPrefs.KEY_SHOW_VOLTAGE, value)

    fun getShowCurrent(): Boolean =
        OverlayPrefs.getBoolean(getApplication(), OverlayPrefs.KEY_SHOW_CURRENT, true)

    fun setShowCurrent(value: Boolean) =
        OverlayPrefs.setBoolean(getApplication(), OverlayPrefs.KEY_SHOW_CURRENT, value)

    fun getShowTemperature(): Boolean =
        OverlayPrefs.getBoolean(getApplication(), OverlayPrefs.KEY_SHOW_TEMPERATURE, false)

    fun setShowTemperature(value: Boolean) =
        OverlayPrefs.setBoolean(getApplication(), OverlayPrefs.KEY_SHOW_TEMPERATURE, value)

    fun getShowLevel(): Boolean =
        OverlayPrefs.getBoolean(getApplication(), OverlayPrefs.KEY_SHOW_LEVEL, false)

    fun setShowLevel(value: Boolean) =
        OverlayPrefs.setBoolean(getApplication(), OverlayPrefs.KEY_SHOW_LEVEL, value)
}
