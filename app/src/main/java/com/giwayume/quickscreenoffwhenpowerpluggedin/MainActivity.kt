package com.giwayume.quickscreenoffwhenpowerpluggedin

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.os.Bundle
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import com.giwayume.quickscreenoffwhenpowerpluggedin.ui.theme.QuickScreenOffWhenPowerPluggedInTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val needsToRequestAdminPermission = MutableLiveData(false)
    private val isPowerScreenLockServiceRunning = MutableLiveData(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainLayout(key = System.currentTimeMillis())
        }
        updateOutsideAppState()
    }

    override fun onResume() {
        super.onResume()
        updateOutsideAppState()
    }

    @Composable
    private fun MainLayout(key: Long) {
        QuickScreenOffWhenPowerPluggedInTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        colors = topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                        ),
                        title = {
                            Text("Don't Turn My Screen On")
                        }
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val needsAdmin = needsToRequestAdminPermission.observeAsState()
                    if (needsAdmin.value == true) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp, 14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Administrator permissions are required in order to allow this app to keep your screen off when plugging the phone in to power turns the screen on.")
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Button(
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.errorContainer
                                        ),
                                        onClick = {
                                            requestAdminPermission()
                                        }
                                    ) {
                                        Text("Request Administrator Permission")
                                    }
                                }
                            }
                        }
                    } else {
                        val isMonitoringPowerPlugs =
                            isPowerScreenLockServiceRunning.observeAsState()
                        if (isMonitoringPowerPlugs.value == true) {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp, 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Power plug is currently being monitored. The screen should remain off when it was off before you plugged in power.")
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Button(
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.errorContainer
                                            ),
                                            onClick = {
                                                stopPowerScreenLockService()
                                            }
                                        ) {
                                            Text("Stop Monitoring")
                                        }
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp, 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("This app is currently not doing anything.")
                                }
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    onClick = {
                                        startPowerScreenLockService()
                                    }
                                ) {
                                    Text("Start Monitoring Power Plug")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateOutsideAppState() {
        needsToRequestAdminPermission.value = queryRequestAdminPermission()
        isPowerScreenLockServiceRunning.value = queryPowerScreenLockServiceRunning()
    }

    private fun queryRequestAdminPermission(): Boolean {
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, DeviceAdminReceiver::class.java)
        return !devicePolicyManager.isAdminActive(componentName)
    }

    @Suppress("DEPRECATION") // Deprecated for third party services.
    private fun queryPowerScreenLockServiceRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val activityServices = activityManager.getRunningServices(Integer.MAX_VALUE)
        return activityServices.any {
            it.service.className == PowerScreenLockService::class.qualifiedName
        }
    }

    private fun lockScreen() {
        println("Lock screen")
        val devicePolicyManager =
            getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, DeviceAdminReceiver::class.java)
        if (devicePolicyManager.isAdminActive(componentName)) {
            devicePolicyManager.lockNow()
        }
    }

    private fun requestAdminPermission() {
        println("Request admin")
        val context = this@MainActivity
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(this, DeviceAdminReceiver::class.java)

        if (!devicePolicyManager.isAdminActive(componentName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Enable Device Admin to manage screen lock."
            )
            context.startActivity(intent)
        }
    }

    private fun startPowerScreenLockService() {
        startForegroundService(Intent(this, PowerScreenLockService::class.java))
        isPowerScreenLockServiceRunning.value = true
    }

    private fun stopPowerScreenLockService() {
        stopService(Intent(this, PowerScreenLockService::class.java))
        isPowerScreenLockServiceRunning.value = false
    }

}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    QuickScreenOffWhenPowerPluggedInTheme {
        Greeting("Android")
    }
}