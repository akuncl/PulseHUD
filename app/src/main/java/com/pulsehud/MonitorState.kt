package com.pulsehud

import android.content.Context
import android.graphics.Color
import java.util.ArrayDeque

object MonitorState {
    private val lock = Any()
    private val eventTimes = ArrayDeque<Long>()
    @Volatile var totalClicks: Long = 0
        private set
    @Volatile var peakCps: Int = 0
        private set

    fun recordClick(context: Context) {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            totalClicks++
            eventTimes.addLast(now)
            trim(now, 10_000L)
            context.getSharedPreferences("pulsehud", Context.MODE_PRIVATE)
                .edit().putLong("total_clicks", totalClicks).apply()
        }
    }

    fun restore(context: Context) {
        totalClicks = context.getSharedPreferences("pulsehud", Context.MODE_PRIVATE)
            .getLong("total_clicks", 0L)
    }

    fun currentCps(windowMs: Long): Int {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            trim(now, maxOf(windowMs, 10_000L))
            val cutoff = now - windowMs
            val count = eventTimes.count { it >= cutoff }
            val cps = (count * 1000.0 / windowMs).toInt()
            if (cps > peakCps) peakCps = cps
            return cps
        }
    }

    fun resetSession(context: Context) {
        synchronized(lock) {
            totalClicks = 0
            peakCps = 0
            eventTimes.clear()
        }
        context.getSharedPreferences("pulsehud", Context.MODE_PRIVATE)
            .edit().putLong("total_clicks", 0L).apply()
    }

    private fun trim(now: Long, retentionMs: Long) {
        val cutoff = now - retentionMs
        while (eventTimes.isNotEmpty() && eventTimes.peekFirst() < cutoff) {
            eventTimes.removeFirst()
        }
    }
}

object HudPrefs {
    fun prefs(context: Context) = context.getSharedPreferences("pulsehud", Context.MODE_PRIVATE)
    fun bool(context: Context, key: String, default: Boolean = true) =
        prefs(context).getBoolean(key, default)
    fun int(context: Context, key: String, default: Int) =
        prefs(context).getInt(key, default)
    fun str(context: Context, key: String, default: String) =
        prefs(context).getString(key, default) ?: default
    fun put(context: Context, key: String, value: Any) {
        val e = prefs(context).edit()
        when (value) {
            is Boolean -> e.putBoolean(key, value)
            is Int -> e.putInt(key, value)
            is Long -> e.putLong(key, value)
            is String -> e.putString(key, value)
        }
        e.apply()
    }
}
