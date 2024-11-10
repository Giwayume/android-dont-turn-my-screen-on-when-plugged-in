package com.giwayume.quickscreenoffwhenpowerpluggedin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlin.math.abs

class PowerScreenLockService: Service() {
    private lateinit var powerReceiver: BroadcastReceiver
    private val notificationChannelId = "PowerConnectionServiceChannel"

    override fun onCreate() {
        super.onCreate()

        createPowerReceiver()
        registerPowerReceiver()

        val notification = createNotification()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterPowerReceiver()
        Toast.makeText(this, "\"Don't Turn My Screen On When Plugged In\" service as stopped.", Toast.LENGTH_SHORT).show()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            notificationChannelId,
            "Don't turn off my screen channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return Notification.Builder(this, notificationChannelId)
            .setContentTitle("Power Connection Monitoring")
            .setContentText("Monitoring power connection status.")
            .setSmallIcon(R.drawable.ic_notification)
            .build()
    }

    private fun createPowerReceiver() {
        powerReceiver = object : BroadcastReceiver() {
            private var powerEventTimestamp: Long = 0L
            private var screenOnEventTimestamp: Long = 0L

            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        powerEventTimestamp = System.currentTimeMillis()
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        powerEventTimestamp = System.currentTimeMillis()
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        screenOnEventTimestamp = System.currentTimeMillis()
                    }
                }

                if (abs(powerEventTimestamp - screenOnEventTimestamp) < 500L) {
                    val devicePolicyManager = context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    devicePolicyManager.lockNow()
                }
            }
        }
    }

    private fun registerPowerReceiver() {
        val powerConnectedFilter = IntentFilter(Intent.ACTION_POWER_CONNECTED)
        val powerDisconnectedFilter = IntentFilter(Intent.ACTION_POWER_DISCONNECTED)
        val screenOnFilter = IntentFilter(Intent.ACTION_SCREEN_ON)

        registerReceiver(powerReceiver, powerConnectedFilter)
        registerReceiver(powerReceiver, powerDisconnectedFilter)
        registerReceiver(powerReceiver, screenOnFilter)
    }

    private fun unregisterPowerReceiver() {
        unregisterReceiver(powerReceiver)
    }
}