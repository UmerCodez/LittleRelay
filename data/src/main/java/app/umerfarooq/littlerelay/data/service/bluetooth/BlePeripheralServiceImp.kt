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
package app.umerfarooq.littlerelay.data.service.bluetooth

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.umerfarooq.littlerelay.data.mapper.toDomain
import app.umerfarooq.littlerelay.domain.model.AdvertiseMode
import app.umerfarooq.littlerelay.domain.model.AdvertiseTxPower
import app.umerfarooq.littlerelay.domain.model.BLEPeripheralServiceInfo
import app.umerfarooq.littlerelay.domain.model.BleDevice
import app.umerfarooq.littlerelay.domain.repository.SettingRepository
import app.umerfarooq.littlerelay.domain.service.bluetooth.AdvertiseState
import app.umerfarooq.littlerelay.domain.service.bluetooth.BlePeripheralService
import app.umerfarooq.littlerelay.domain.service.bluetooth.BlePeripheralServiceState
import app.umerfarooq.littlerelay.domain.service.bluetooth.BluetoothAdvertisementFailedException
import app.umerfarooq.littlerelay.domain.service.bluetooth.GattServerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@SuppressLint("MissingPermission")
@AndroidEntryPoint
class BlePeripheralServiceImp : Service(), BlePeripheralService {

    @Inject
    lateinit var settingRepository: SettingRepository

    private val _gattServerState = MutableStateFlow<GattServerState>(GattServerState.Close)
    override val gattServerState = _gattServerState.asStateFlow()

    private val _advertiseState = MutableStateFlow<AdvertiseState>(AdvertiseState.Stopped)
    override val advertiseState = _advertiseState.asStateFlow()

    private val _serviceState = MutableStateFlow<BlePeripheralServiceState>(
        BlePeripheralServiceState.Stopped
    )
    override val serviceState = _serviceState.asStateFlow()


    private val _connectedDevices: MutableStateFlow<Set<BleDevice>> = MutableStateFlow(emptySet())
    override val connectedDevices = _connectedDevices.asStateFlow()

    private val remoteDevices = MutableStateFlow<Set<RemoteDevice>>(emptySet())


    private val _incomingData = MutableSharedFlow<ByteArray>()
    override val incomingData = _incomingData.asSharedFlow()

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val bluetoothManager: BluetoothManager by lazy { getSystemService(BLUETOOTH_SERVICE) as BluetoothManager }
    private val bluetoothAdapter: BluetoothAdapter by lazy{ bluetoothManager.adapter }

    private var advertiser: BluetoothLeAdvertiser? = null
    private var isAdvertising = false
    private var gattServer: BluetoothGattServer? = null

    private val myServiceUUID = UUID.fromString(BLEPeripheralServiceInfo.SERVICE_UUID)

    private val notificationManager: NotificationManager by lazy<NotificationManager>{ getSystemService(NotificationManager::class.java) }


    private val notifiableCharacteristicUUID =
        UUID.fromString(BLEPeripheralServiceInfo.NOTIFIABLE_CHARACTERISTIC_UUID)

    // TODO :
    // Currently this characteristic supports NOTIFICATION only (not INDICATION).
    // Indications require waiting for a client confirmation (ACK) before sending
    // the next update, which would require explicit queuing and flow-control logic.
    // Since data arriving from the broker may be continuous, notifications are
    // used for now to avoid blocking and simplify delivery.
    private val notifiableCharacteristic = BluetoothGattCharacteristic(
        notifiableCharacteristicUUID,
        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
        BluetoothGattCharacteristic.PERMISSION_READ
    )


    private val advertiseCallBack = object : AdvertiseCallback() {

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            isAdvertising = true

            setAdvertiseState(AdvertiseState.Started(settingsInEffect.toDomain()))

        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(TAG, "onStartFailure: $errorCode")
            isAdvertising = false

            setAdvertiseState(
                AdvertiseState.Error(
                    BluetoothAdvertisementFailedException("Advertisement failed with error code: $errorCode")
                )
            )
        }

    }


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(
            TAG,
            "onStartCommand() called with: intent = [$intent], flags = [$flags], startId = [$startId]"
        )


        setServiceState(BlePeripheralServiceState.Running)


        val notificationBuilder =
            NotificationCompat.Builder(
                applicationContext,
                NOTIFICATION_CHANNEL_ID
            )
                .apply {
                    setSmallIcon(R.drawable.stat_notify_sync)
                    setContentTitle("BLE Peripheral Service")
                    setContentText("Running")
                    setPriority(NotificationCompat.PRIORITY_HIGH)
                    setAutoCancel(false)
                    setOngoing(true)
                }


        val notification = notificationBuilder.build()

        startForeground(ON_GOING_NOTIFICATION_ID, notification)

        return START_NOT_STICKY
    }


    override fun advertiseService() {
        startAdvertising()
    }

    override fun startService() {
        ContextCompat.startForegroundService(
            applicationContext,
            Intent(applicationContext, BlePeripheralServiceImp::class.java)
        )
    }

    override fun stopService() {
        stopAdvertising()
        closeGatServer()
        stopForeground()
        stopSelf()
        setServiceState(BlePeripheralServiceState.Stopped)

    }

    @RequiresPermission(value = Manifest.permission.BLUETOOTH_CONNECT)
    override fun disconnectFromDevice(device: BleDevice) {
       scope.launch {
           remoteDevices.value.filter { it.domainModel.isConnected }.forEach { connectedDevice ->
               if(connectedDevice.bluetoothDevice.address == device.address){
                   gattServer?.cancelConnection(connectedDevice.bluetoothDevice)
                   // cancelConnection doesn't call backs STATE_DISCONNECTED in gatt server callback
                   _connectedDevices.value = _connectedDevices.value.filter { it.address != device.address }.toSet()
               }
           }
       }
    }

    override fun openGatServer() {
        scope.launch {
            gattServer =
                bluetoothManager.openGattServer(applicationContext, gattServerCallback)


            val service =
                BluetoothGattService(myServiceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)


            val writableCharacteristicUUID =
                UUID.fromString(BLEPeripheralServiceInfo.WRITEABLE_CHARACTERISTIC_UUID)
            val writableCharacteristic = BluetoothGattCharacteristic(
                writableCharacteristicUUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )


            // TODO: Check and remove descriptor if not needed
            val notifiableCharacteristicDescriptorUUID =
                UUID.fromString(BLEPeripheralServiceInfo.NOTIFIABLE_DESCRIPTOR_UUID)


            // Add descriptor for enabling notifications
            val descriptor = BluetoothGattDescriptor(
                notifiableCharacteristicDescriptorUUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )

            notifiableCharacteristic.addDescriptor(descriptor)

            service.addCharacteristic(notifiableCharacteristic)
            service.addCharacteristic(writableCharacteristic)
            gattServer?.addService(service)


            setGatServerState(GattServerState.Open)


            if (gattServer == null) {
                setGatServerState(GattServerState.Error(Exception("Gatt server is null")))
            }
        }

    }

    override fun closeGatServer() {

        scope.launch {
            bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)
                ?.forEach { connectedDevice ->
                    gattServer?.cancelConnection(connectedDevice)
                }
            gattServer?.clearServices()
            gattServer?.close()
            _connectedDevices.value = emptySet()
            setGatServerState(GattServerState.Close)
        }

    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE])
    override fun stopAdvertising() {
        advertiser?.stopAdvertising(advertiseCallBack)
        isAdvertising = false
        setAdvertiseState(AdvertiseState.Stopped)

    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(
            device: BluetoothDevice,
            status: Int,
            newState: Int
        ) {
            super.onConnectionStateChange(device, status, newState)

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                remoteDevices.value += RemoteDevice(
                    domainModel = device.toDomain().copy(isConnected = true),
                    bluetoothDevice = device
                )


            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                remoteDevices.value = remoteDevices.value.filter { it.domainModel.address != device.address }.toSet()
            }

            _connectedDevices.value =
                remoteDevices.value.filter { it.domainModel.isConnected }.map { it.domainModel }.toSet()

        }


        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )

            // | Write Type        | responseNeeded | sendResponse() | Client gets confirmation |
            //| ----------------- | -------------- | -------------- | ------------------------ |
            //| WRITE             | true           |  yes          |  yes                    |
            //| WRITE_NO_RESPONSE | false          |  no           |  no                     |

            scope.launch {

                if (responseNeeded) {
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        offset,
                        null
                    )
                }


                if (value != null)
                    _incomingData.emit(value)
            }

        }


        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?, requestId: Int,
            descriptor: BluetoothGattDescriptor?, preparedWrite: Boolean,
            responseNeeded: Boolean, offset: Int, value: ByteArray?
        ) {
            Log.d(TAG, "onDescriptorWriteRequest() called with: device = [$device], requestId")
            if (responseNeeded) {
                gattServer?.sendResponse(
                    device, requestId,
                    BluetoothGatt.GATT_SUCCESS, 0, null
                )
            }
        }
    }


    //@RequiresPermission(value = Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun startAdvertising() {

        scope.launch {

            if (isAdvertising)
                return@launch

            // TODO: prompt user to enable bluetooth
            // Will return null if Bluetooth is turned off or if Bluetooth LE Advertising is not supported on this device.
            advertiser = bluetoothAdapter.bluetoothLeAdvertiser

            if (advertiser == null) {
                Log.e(TAG, "Advertiser is NULL")
                return@launch
            }

            /*        if (!bluetoothAdapter.isMultipleAdvertisementSupported()) {
            return
        }*/


            val bleConfig = settingRepository.bleConfig.first()

            val advertiseSettings = AdvertiseSettings.Builder()
                .setAdvertiseMode(
                    when (bleConfig.bleAdvertiseSetting.mode) {
                        AdvertiseMode.LOW_POWER -> AdvertiseSettings.ADVERTISE_MODE_LOW_POWER
                        AdvertiseMode.BALANCED -> AdvertiseSettings.ADVERTISE_MODE_BALANCED
                        AdvertiseMode.LOW_LATENCY -> AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
                    }
                )
                .setConnectable(true)
                .setTxPowerLevel(
                    when (bleConfig.bleAdvertiseSetting.txPowerLevel) {
                        AdvertiseTxPower.ULTRA_LOW -> AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW
                        AdvertiseTxPower.LOW -> AdvertiseSettings.ADVERTISE_TX_POWER_LOW
                        AdvertiseTxPower.MEDIUM -> AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM
                        AdvertiseTxPower.HIGH -> AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
                    }
                )
                .setTimeout(bleConfig.bleAdvertiseSetting.timeout)
                .build()

            val advertiseData = AdvertiseData.Builder()
                // A long device name can cause advertisement failures.
                // TODO: Prompt the user to set a shorter name in Bluetooth settings
                // or exclude the device name from the advertisement.
                .setIncludeDeviceName(bleConfig.bleAdvertiseSetting.includeDeviceName)
                .addServiceUuid(ParcelUuid(myServiceUUID))
                .build()

            advertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallBack)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()
        scope.cancel()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    override fun sendToConnectedDevices(data: ByteArray) {

        try {

            bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)
                ?.forEach { connectedDevice ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                        gattServer?.notifyCharacteristicChanged(
                            connectedDevice,
                            notifiableCharacteristic,
                            false,
                            data
                        )

                    } else {
                        notifiableCharacteristic.value = data
                        gattServer?.notifyCharacteristicChanged(
                            connectedDevice,
                            notifiableCharacteristic,
                            false
                        )
                    }
                }

        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    private fun setGatServerState(gattServerState: GattServerState){
        _gattServerState.value = gattServerState
    }
    private fun setAdvertiseState(advertiseState: AdvertiseState){
        _advertiseState.value = advertiseState
    }
    private fun setServiceState(serviceState: BlePeripheralServiceState){
        _serviceState.value = serviceState
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "createNotificationChannel()")
            val name: CharSequence = "Little Replay"
            val description = "Notifications from Little Relay BLE Service"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this

            notificationManager.createNotificationChannel(channel)
        }
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
        val service: BlePeripheralServiceImp
            get() = this@BlePeripheralServiceImp // Return this instance of LocalService so clients can call public methods

    }

    companion object {
        private val TAG: String = BlePeripheralServiceImp::class.java.getSimpleName()
        const val NOTIFICATION_CHANNEL_ID = "BLE Peripheral Service"

        // cannot be zero
        const val ON_GOING_NOTIFICATION_ID = 584
    }

}

