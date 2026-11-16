package com.pulsehud

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.math.roundToInt

class OverlayService : Service() {
    companion object {
        const val ACTION_START = "com.pulsehud.START"
        const val ACTION_STOP = "com.pulsehud.STOP"
        const val ACTION_EDIT = "com.pulsehud.EDIT"
        const val ACTION_LIVE = "com.pulsehud.LIVE"
        private const val CHANNEL = "pulsehud_overlay"
    }

    private lateinit var wm: WindowManager
    private var label: TextView? = null
    private var params: WindowManager.LayoutParams? = null
    private var editMode = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val update = object : Runnable {
        override fun run() {
            updateText()
            handler.postDelayed(this, 200L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        MonitorState.restore(this)
        createChannel()
        startForeground(41, notification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            ACTION_EDIT -> setEditMode(true)
            ACTION_LIVE -> setEditMode(false)
            ACTION_START, null -> if (label == null) createOverlay()
        }
        if (label == null) createOverlay()
        return START_STICKY
    }

    private fun createOverlay() {
        val p = HudPrefs.prefs(this)
        val x = p.getInt("x", 24)
        val y = p.getInt("y", 120)
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                   else WindowManager.LayoutParams.TYPE_PHONE
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
        label = TextView(this).apply {
            textSize = p.getInt("font_size", 14).toFloat()
            setTextColor(Color.parseColor(p.getString("text_color", "#FFFFFF")))
            setPadding(14, 9, 14, 9)
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 18f
                setColor(Color.argb(
                    (p.getInt("opacity", 75) * 2.55f).roundToInt().coerceIn(0,255),
                    18, 22, 34
                ))
                setStroke(1, Color.parseColor("#445A70"))
            }
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            text = "PulseHUD"
        }
        try { wm.addView(label, params) } catch (_: Exception) { stopSelf(); return }
        setEditMode(false)
        handler.post(update)
    }

    private fun setEditMode(enabled: Boolean) {
        editMode = enabled
        val v = label ?: return
        val lp = params ?: return
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            (if (enabled) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        try { wm.updateViewLayout(v, lp) } catch (_: Exception) {}
        v.setOnTouchListener(if (enabled) dragListener else null)
        v.alpha = if (enabled) 0.85f else 1f
        updateText()
    }

    private val dragListener = View.OnTouchListener { view, event ->
        val lp = params ?: return@OnTouchListener false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                view.tag = floatArrayOf(event.rawX - lp.x, event.rawY - lp.y)
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val delta = view.tag as? FloatArray ?: return@OnTouchListener true
                lp.x = (event.rawX - delta[0]).toInt()
                lp.y = (event.rawY - delta[1]).toInt()
                try { wm.updateViewLayout(view, lp) } catch (_: Exception) {}
                true
            }
            MotionEvent.ACTION_UP -> {
                HudPrefs.put(this, "x", lp.x)
                HudPrefs.put(this, "y", lp.y)
                true
            }
            else -> true
        }
    }

    private fun updateText() {
        val v = label ?: return
        val windowMs = HudPrefs.int(this, "interval_ms", 1000).coerceAtLeast(100)
        val cps = MonitorState.currentCps(windowMs.toLong())
        val p = HudPrefs.prefs(this)
        val parts = mutableListOf<String>()
        if (p.getBoolean("show_cps", true)) {
            parts += "CPS ${cps}  PEAK ${MonitorState.peakCps}  TOTAL ${MonitorState.totalClicks}"
        }
        if (p.getBoolean("show_fps", true)) parts += "FPS —"
        if (p.getBoolean("show_battery", true)) {
            val battery = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = battery?.getIntExtra("level", -1) ?: -1
            val scale = battery?.getIntExtra("scale", 100) ?: 100
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
            val temp = battery?.getIntExtra("temperature", -1) ?: -1
            val tempText = if (temp >= 0) "%.1f°C".format(temp / 10f) else "Unavailable"
            parts += "BAT ${if (pct >= 0) "$pct%" else "—"}  $tempText"
        }
        if (editMode) parts.add(0, "EDIT MODE · drag me")
        v.text = parts.joinToString("\n").ifBlank { "PulseHUD · no widgets enabled" }
        v.textSize = p.getInt("font_size", 14).toFloat()
        try { v.setTextColor(Color.parseColor(p.getString("text_color", "#FFFFFF"))) } catch (_: Exception) {}
        v.gravity = when (p.getString("alignment", "left")) {
            "center" -> Gravity.CENTER
            "right" -> Gravity.RIGHT
            else -> Gravity.LEFT
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "PulseHUD overlay", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun notification(): Notification =
        NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("PulseHUD is running")
            .setContentText("Floating performance overlay active")
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        handler.removeCallbacks(update)
        try { label?.let { wm.removeView(it) } } catch (_: Exception) {}
        label = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
