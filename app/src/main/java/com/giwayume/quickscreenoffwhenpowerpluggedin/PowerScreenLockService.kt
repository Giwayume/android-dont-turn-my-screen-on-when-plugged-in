package com.giwayume.quickscreenoffwhenpowerpluggedin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PowerScreenLockService: Service() {
    private lateinit var powerReceiver: BroadcastReceiver
    private val notificationChannelId = "PowerConnectionServiceChannel"

    private lateinit var devicePolicyManager: DevicePolicyManager

    private var lastKnownUserBrightness: Int = 0

    private var powerEventTimestamp: Long = 0L
    private var screenOnEventTimestamp: Long = 1L
    private var screenOffEventTimestamp: Long = 0L
    private var automatedLockTimestamp: Long = 0L

    private val lockHandler = Handler(Looper.getMainLooper())
    private val lockRunnable = Runnable {
        val screenOnOffset = abs(powerEventTimestamp - screenOnEventTimestamp)
        if (screenOnOffset < 250L) {
            automatedLockTimestamp = System.currentTimeMillis()
            devicePolicyManager.lockNow()
            setSystemScreenBrightness(1)
        }
    }
    private val brightnessResetRunnable = Runnable {
        val screenOnOffset = abs(powerEventTimestamp - screenOnEventTimestamp)
        if (screenOnOffset >= 250L) {
            setSystemScreenBrightness(lastKnownUserBrightness)
        }
    }

    override fun onCreate() {
        super.onCreate()

        devicePolicyManager = this.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
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
                    Intent.ACTION_SCREEN_OFF -> {
                        screenOffEventTimestamp = System.currentTimeMillis()
                    }
                }

                if (intent?.action == Intent.ACTION_POWER_CONNECTED || intent?.action == Intent.ACTION_POWER_DISCONNECTED || intent?.action == Intent.ACTION_SCREEN_ON) {
                    val screenOnOffset = abs(powerEventTimestamp - screenOnEventTimestamp)
                    if (screenOnOffset < 250L) {
                        val currentScreenBrightness = getSystemScreenBrightness()
                        if (currentScreenBrightness > 1) {
                            lastKnownUserBrightness = currentScreenBrightness
                        }
                        setSystemScreenBrightness(1)
                    }
                }

                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    val currentScreenBrightness = getSystemScreenBrightness()
                    val automatedLockOffset = abs(screenOffEventTimestamp - automatedLockTimestamp)
                    if (currentScreenBrightness > 1 || automatedLockOffset > 1500) {
                        lastKnownUserBrightness = currentScreenBrightness
                    }
                }

                if (intent?.action == Intent.ACTION_SCREEN_ON) {
                    lockHandler.removeCallbacks(brightnessResetRunnable)
                    lockHandler.postDelayed(brightnessResetRunnable, 50)
                }

                if (intent?.action == Intent.ACTION_POWER_CONNECTED || intent?.action == Intent.ACTION_POWER_DISCONNECTED) {
                    lockHandler.removeCallbacks(lockRunnable)
                    lockHandler.postDelayed(lockRunnable, 750)
                }
            }
        }
    }

    private fun registerPowerReceiver() {
        val powerConnectedFilter = IntentFilter(Intent.ACTION_POWER_CONNECTED)
        val powerDisconnectedFilter = IntentFilter(Intent.ACTION_POWER_DISCONNECTED)
        val screenOnFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        val screenOffFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)

        registerReceiver(powerReceiver, powerConnectedFilter)
        registerReceiver(powerReceiver, powerDisconnectedFilter)
        registerReceiver(powerReceiver, screenOnFilter)
        registerReceiver(powerReceiver, screenOffFilter)
    }

    private fun unregisterPowerReceiver() {
        unregisterReceiver(powerReceiver)
    }

    private fun getSystemScreenBrightness(): Int {
        val resolver: ContentResolver = contentResolver
        return Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 0)
    }

    private fun setSystemScreenBrightness(brightness: Int) {
        println("set brightness $brightness")
        val resolver: ContentResolver = contentResolver
        if (Settings.System.canWrite(this)) {
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
            println("brightness set?")
        }
    }
}