package com.example.gameboost

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.ServiceCompat

class GameBoostService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var am: ActivityManager
    private lateinit var nm: NotificationManager
    private var overlay: TextView? = null
    private var expanded = false
    private var previousFilter = NotificationManager.INTERRUPTION_FILTER_ALL
    private var dndChanged = false
    private val handler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            overlay?.text = stats()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WindowManager::class.java)
        am = getSystemService(ActivityManager::class.java)
        nm = getSystemService(NotificationManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        val gamePkg = intent?.getStringExtra("pkg")
        boost(gamePkg)
        enableDnd()
        if (overlay == null) showOverlay()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val channelId = "gameboost"
        nm.createNotificationChannel(
            NotificationChannel(channelId, "GameBoost", NotificationManager.IMPORTANCE_LOW)
        )
        val notif = Notification.Builder(this, channelId)
            .setContentTitle("GameBoost activo")
            .setContentText("Modo juego en marcha")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        ServiceCompat.startForeground(
            this, 1, notif,
            if (Build.VERSION.SDK_INT >= 34)
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
    }

    /** Cierra procesos en segundo plano de apps de usuario (el sistema decide cuáles). */
    private fun boost(gamePkg: String?) {
        packageManager.getInstalledApplications(0)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .filter { it.packageName != packageName && it.packageName != gamePkg }
            .forEach { am.killBackgroundProcesses(it.packageName) }
    }

    private fun enableDnd() {
        if (nm.isNotificationPolicyAccessGranted && !dndChanged) {
            previousFilter = nm.currentInterruptionFilter
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            dndChanged = true
        }
    }

    private fun showOverlay() {
        val tv = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(24, 12, 24, 12)
            setBackgroundColor(0x99000000.toInt())
            setOnClickListener { expanded = !expanded; text = stats() }
            setOnLongClickListener { stopSelf(); true }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 48 }
        wm.addView(tv, lp)
        overlay = tv
    }

    private fun stats(): String {
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val usedPct = 100 - (mi.availMem * 100 / mi.totalMem)
        val b = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = (b?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val level = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        return if (expanded)
            "RAM: $usedPct%\nBatería: $level% · $temp°C\nTérmico: ${thermal()}\n(toca: reducir · mantén: salir)"
        else "RAM $usedPct% · $temp°C"
    }

    private fun thermal(): String {
        if (Build.VERSION.SDK_INT < 29) return "n/d"
        val pm = getSystemService(PowerManager::class.java)
        return when (pm.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE, PowerManager.THERMAL_STATUS_LIGHT -> "OK"
            PowerManager.THERMAL_STATUS_MODERATE -> "Caliente"
            else -> "¡Crítico!"
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        overlay?.let { runCatching { wm.removeView(it) } }
        overlay = null
        if (dndChanged && nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(previousFilter)
        }
        super.onDestroy()
    }
}
