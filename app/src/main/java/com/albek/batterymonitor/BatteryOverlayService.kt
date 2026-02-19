package com.albek.batterymonitor

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.TextView
import android.widget.FrameLayout
import android.view.ViewConfiguration
import kotlin.math.abs
import androidx.core.app.NotificationCompat

class BatteryOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateOverlay()
            updateNotification()
            handler.postDelayed(this, 100)
        }
    }

    private val CHANNEL_ID = "BatteryMonitorChannel"
    private val NOTIFICATION_ID = 1
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder

    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Battery Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // أيقونة موجودة في كل الإصدارات
            .setContentTitle("Battery Monitor")
            .setContentText(getString(R.string.overlay_loading))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, notificationBuilder.build())

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!this::windowManager.isInitialized) {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }

        if (overlayView == null) {
            addOverlayView()
        }

        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)

        return START_STICKY
    }

    private fun addOverlayView() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.battery_overlay, FrameLayout(this), false)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 50
        params.y = 200

        overlayView?.setOnTouchListener(object : View.OnTouchListener {
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var hasMoved = false
            var downX = 0f
            var downY = 0f
            val touchSlop = ViewConfiguration.get(this@BatteryOverlayService).scaledTouchSlop

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        downX = event.rawX
                        downY = event.rawY
                        hasMoved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()

                        if (!hasMoved) {
                            hasMoved =
                                abs(event.rawX - downX) > touchSlop || abs(event.rawY - downY) > touchSlop
                        }

                        if (params.gravity and Gravity.END == Gravity.END) {
                            params.x = initialX - dx // <--- عكس الحركة أفقياً
                        } else {
                            params.x = initialX + dx
                        }
                        params.y = initialY + dy
                        windowManager.updateViewLayout(overlayView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!hasMoved) {
                            v?.performClick()
                        }
                        return true
                    }
                }
                return false
            }
        })


        overlayView?.let { windowManager.addView(it, params) }
    }

    private fun formatBatteryText(intent: Intent?): String {
        if (intent == null) return getString(R.string.overlay_loading)

        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val temperatureTenthC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)

        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val currentMa = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000

        val showVoltage = OverlayPrefs.getBoolean(this, OverlayPrefs.KEY_SHOW_VOLTAGE, true)
        val showCurrent = OverlayPrefs.getBoolean(this, OverlayPrefs.KEY_SHOW_CURRENT, true)
        val showTemperature = OverlayPrefs.getBoolean(this, OverlayPrefs.KEY_SHOW_TEMPERATURE, false)
        val showLevel = OverlayPrefs.getBoolean(this, OverlayPrefs.KEY_SHOW_LEVEL, false)

        val lines = ArrayList<String>(4)

        if (showVoltage && voltageMv > 0) {
            lines += getString(R.string.overlay_voltage_format, voltageMv / 1000.0)
        }

        if (showCurrent) {
            lines += getString(R.string.overlay_current_format, currentMa)
        }

        if (showTemperature && temperatureTenthC != Int.MIN_VALUE) {
            lines += getString(R.string.overlay_temperature_format, temperatureTenthC / 10.0f)
        }

        if (showLevel && level >= 0 && scale > 0) {
            val percent = (level * 100) / scale
            lines += getString(R.string.overlay_level_format, percent)
        }

        if (lines.isEmpty()) {
            lines += getString(R.string.overlay_loading)
        }

        return lines.joinToString("\n")
    }

    private fun updateOverlay() {
        val tvBattery = overlayView?.findViewById<TextView>(R.id.tvBattery)
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        tvBattery?.text = formatBatteryText(intent)
    }

    private fun updateNotification() {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val text = formatBatteryText(intent)

        notificationBuilder.setContentText(text)
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        overlayView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
