/*
 *     This file is a part of LittleRelay (https://www.github.com/UmerCodez/LittleRelay)
 *     Copyright (C) 2026 Umer Farooq (umerfarooq2383@gmail.com)
 *
 *     LittleRelay is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LittleRelay is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LittleRelay. If not, see <https://www.gnu.org/licenses/>.
 *
 */
package app.umerfarooq.littlerelay.data.service.bridge

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.umerfarooq.littlerelay.domain.model.BridgeLog
import app.umerfarooq.littlerelay.domain.model.LogType
import app.umerfarooq.littlerelay.domain.service.BoundedServiceInstanceProvider
import app.umerfarooq.littlerelay.domain.service.bluetooth.BleCentralService
import app.umerfarooq.littlerelay.domain.service.bluetooth.BleCentralServiceState
import app.umerfarooq.littlerelay.domain.service.bluetooth.BlePeripheralService
import app.umerfarooq.littlerelay.domain.service.bluetooth.BlePeripheralServiceState
import app.umerfarooq.littlerelay.domain.service.bridge.BleToMqttBridgeService
import app.umerfarooq.littlerelay.domain.service.bridge.BleToMqttBridgeServiceState
import app.umerfarooq.littlerelay.domain.service.mqtt.MqttConnectionState
import app.umerfarooq.littlerelay.domain.service.mqtt.MqttService
import app.umerfarooq.littlerelay.domain.service.mqtt.MqttServiceState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BleToMqttBridgeServiceImp: Service(), BleToMqttBridgeService {

    @Inject
    lateinit var boundedServiceInstanceProvider: BoundedServiceInstanceProvider

    private val _serviceState = MutableStateFlow<BleToMqttBridgeServiceState>(
        BleToMqttBridgeServiceState.Stopped
    )
    override val serviceState = _serviceState.asStateFlow()


    private val _logList = MutableStateFlow<List<BridgeLog>>(emptyList())
    override val logs = _logList.asStateFlow()




    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val notificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

    private data class MqttBlePeripheralBridge(
        val blePeripheralService: BlePeripheralService,
        val mqttService: MqttService
    )

    private data class MqttBleCentralBridge(
        val bleCentralService: BleCentralService,
        val mqttService: MqttService
    )



    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()

        scope.launch {
            launch {
                serviceState.collect{
                    log("Bridge Service state: $it")
                }
            }
            launch {
                boundedServiceInstanceProvider.mqttService.filterNotNull()
                    .flatMapLatest { it.connectionState }.collect { mqttConnectionState ->
                    when (mqttConnectionState) {
                        MqttConnectionState.Connected -> log("Connected to Broker")
                        MqttConnectionState.Disconnected -> log(
                                type = LogType.Warning,
                                message = "Not connected to Broker"
                        )

                        else -> {}
                    }
                }
            }

        }


        scope.launch {

            // Combine the latest instances of:
            // 1) MQTT service
            // 2) BLE Peripheral service
            //
            // The block is re-executed whenever either service reference changes.
            // Note: The services instances are immediately available when app starts but in very rare case these bounded instance may get lost and provided again by the boundedServiceInstanceProvider
            combine(
                // NOTE:
                // `combine` emits only after all upstream flows have emitted at least once.
                // This is safe here because both mqttService and blePeripheralService are StateFlows,
                // which always hold and immediately emit their latest value to collectors.

                boundedServiceInstanceProvider.mqttService,
                boundedServiceInstanceProvider.blePeripheralService
            ) { mqttService, bleService ->

                // Only create the bridge when BOTH services are available.
                // If either service is null, no bridge is created.
                if (mqttService != null && bleService != null) {
                    MqttBlePeripheralBridge(
                        blePeripheralService = bleService,
                        mqttService = mqttService
                    )
                } else {
                    null
                }
            }
                .filterNotNull()
                .distinctUntilChanged() // Prevent re-creating the bridge if the same instance is emitted again

                // Switch to a new bridging flow whenever a NEW bridge instance appears (rare case).
                // Old flows are automatically cancelled.
                .flatMapLatest { bridge ->

                    // Flow 1: MQTT → BLE
                    val mqttToBle =
                        bridge.mqttService.incomingData
                            .onEach { data ->
                                log("Received ${data.size} bytes from Broker")
                                log("Sending ${data.size} bytes to connected BLE devices")
                                bridge.blePeripheralService.sendToConnectedDevices(data)
                            }

                    // Flow 2: BLE → MQTT
                    val bleToMqtt =
                        bridge.blePeripheralService.incomingData
                            .onEach { data ->
                                log("Received ${data.size} bytes from BLE Device")
                                log("Sending ${data.size} bytes to Broker")
                                bridge.mqttService.sendToBroker(data)
                            }

                    // Run both directions concurrently
                    merge(mqttToBle, bleToMqtt)
                }
                .collect()
        }


        scope.launch {
            combine(
                boundedServiceInstanceProvider.mqttService,
                boundedServiceInstanceProvider.bleCentralService
            ) { mqttService, bleCentralService ->
                if (mqttService != null && bleCentralService != null) {
                    MqttBleCentralBridge(
                        bleCentralService = bleCentralService,
                        mqttService = mqttService
                    )
                } else {
                    null
                }
            }
                .distinctUntilChanged()
                .filterNotNull()
                .flatMapLatest { bridge ->

                    val mqttToBle =
                        bridge.mqttService.incomingData
                            .onEach { data ->
                                log("Received ${data.size} bytes from Broker")
                                log("Sending ${data.size} bytes to connected BLE devices")
                                bridge.bleCentralService.sendToConnectedDevices(data)
                            }


                    val bleToMqtt =
                        bridge.bleCentralService.incomingData
                            .onEach { data ->
                                log("Received ${data.size} bytes from BLE Device")
                                log("Sending ${data.size} bytes to Broker")
                                bridge.mqttService.sendToBroker(data)
                            }

                    merge(mqttToBle, bleToMqtt)
                }
                .collect()
        }


        scope.launch {
            // Stage 1: Ensure all Service Instances are available
            combine(
                boundedServiceInstanceProvider.mqttService,
                boundedServiceInstanceProvider.blePeripheralService,
                boundedServiceInstanceProvider.bleCentralService
            ) { mqttService, blePeripheralService, bleCentralService ->
                // Bundle instances into a Triple to pass them down the chain
                if (mqttService != null && blePeripheralService != null && bleCentralService != null)
                    Triple(mqttService, blePeripheralService, bleCentralService)
                else
                    null
            }.filterNotNull()
                .flatMapLatest { (mqtt, peripheral, central) ->
                    combine(
                        mqtt.serviceState,
                        peripheral.serviceState,
                        central.serviceState
                    ) { mqttState, periState, centState ->

                        val isMqttRunning = mqttState is MqttServiceState.Running
                        val isBleRunning = periState is BlePeripheralServiceState.Running ||
                                centState is BleCentralServiceState.Running

                        // The Bridge Service should only run if MQTT is active
                        // AND there is at least one active BLE central/peripheral service.
                        isMqttRunning && isBleRunning
                    }
                }
                .distinctUntilChanged() // Only trigger when the boolean result flips (true <-> false)
                .collect { shouldStartBridge ->
                    if (shouldStartBridge) {
                        log("Starting Bridge Service")
                        startService()
                    } else {
                        log("Stopping Bridge Service")
                        stopService()
                    }
                }
        }


        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "createNotificationChannel()")
            val name: CharSequence = "Little Replay"
            val description = "Notifications from Little Relay BLE2MQTT Bridge Service"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this

            notificationManager.createNotificationChannel(channel)
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand() called with: intent = [$intent], flags = [$flags], startId = [$startId]")

        val notificationBuilder =
            NotificationCompat.Builder(applicationContext,
                NOTIFICATION_CHANNEL_ID
            )
                .apply {
                    setSmallIcon(R.drawable.stat_notify_sync)
                    setContentTitle("BLE to MQTT Bridge Service")
                    setContentText("Running")
                    setPriority(NotificationCompat.PRIORITY_HIGH)
                    setAutoCancel(false)
                    setOngoing(true)
                }


        val notification = notificationBuilder.build()

        startForeground(ON_GOING_NOTIFICATION_ID, notification)

        _serviceState.value = BleToMqttBridgeServiceState.Running



        return START_NOT_STICKY
    }


    override fun startService() {
        ContextCompat.startForegroundService(
            applicationContext,
            Intent(applicationContext, BleToMqttBridgeServiceImp::class.java)
        )
    }

    override fun stopService() {
        stopForeground()
        stopSelf()
        _serviceState.value = BleToMqttBridgeServiceState.Stopped
    }

    private fun log(message: String, type: LogType = LogType.Info){
        val log = BridgeLog(message = message, type = type)
        _logList.update { currentList ->
            (currentList + log).takeLast(50)
        }

    }

    override fun clearLogs() {

    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()
        scope.cancel()
    }

    @Suppress("DEPRECATION")
    private fun stopForeground() {
        /*
        If the device is running an older version of Android,
        we fallback to stopForeground(true) to remove the service from the foreground and dismiss the ongoing notification.
        Although it shows as deprecated, it should still work as expected on API level 21 (Android 5).
         */

        // for Android 7 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            stopForeground(STOP_FOREGROUND_REMOVE)
        else
        // This method was deprecated in API level 33.
        // Ignore deprecation message as there is no other alternative method for Android 6 and lower
            stopForeground(true)
    }

    // Binder given to clients
    private val binder: IBinder = LocalBinder()
    override fun onBind(intent: Intent?) = binder

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {

        // Return this instance of LocalService so clients can call public methods
        val service: BleToMqttBridgeService
            get() = this@BleToMqttBridgeServiceImp // Return this instance of LocalService so clients can call public methods

    }

    companion object {
        private val TAG: String = BleToMqttBridgeServiceImp::class.java.getSimpleName()
        const val NOTIFICATION_CHANNEL_ID = "Little Replay BLE2MQTT Bridge Notification"

        // cannot be zero
        const val ON_GOING_NOTIFICATION_ID = 971
    }
}