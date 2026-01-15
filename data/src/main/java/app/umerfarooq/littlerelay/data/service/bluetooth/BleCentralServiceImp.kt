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
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.umerfarooq.littlerelay.data.mapper.toDomain
import app.umerfarooq.littlerelay.domain.model.BleCharacteristic
import app.umerfarooq.littlerelay.domain.model.BleDevice
import app.umerfarooq.littlerelay.domain.service.bluetooth.BleCentralService
import app.umerfarooq.littlerelay.domain.service.bluetooth.BleCentralServiceState
import app.umerfarooq.littlerelay.domain.service.bluetooth.BleScanState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BleCentralServiceImp: Service(), BleCentralService {


    private val _serviceState = MutableStateFlow<BleCentralServiceState>(BleCentralServiceState.Stopped)
    override val serviceState = _serviceState.asStateFlow()

    private val _scanState = MutableStateFlow<BleScanState>(BleScanState.Stopped)
    override val scanState = _scanState.asStateFlow()


    private val _scannedDevices = MutableStateFlow<Set<BleDevice>>(emptySet())
    override val scannedDevices = _scannedDevices.asStateFlow()

    private val _connectedDevices = MutableStateFlow<Set<BleDevice>>(emptySet())
    override val connectedDevices = _connectedDevices.asStateFlow()


    private val remoteDevices = MutableStateFlow(emptySet<RemoteDevice>())



    private val writeableCharacteristics: MutableSet<BluetoothGattCharacteristic> = mutableSetOf()


    private val _incomingData = MutableSharedFlow<ByteArray>()
    override val incomingData = _incomingData.asSharedFlow()

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val bluetoothManager by lazy<BluetoothManager> { getSystemService(BluetoothManager::class.java) }
    private val bluetoothAdapter by lazy { bluetoothManager.adapter }
    private var bluetoothLeScanner: BluetoothLeScanner? = null


    private val scanCallback = object : ScanCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi
            val name = device.name ?: "Unknown"

            Log.d(TAG, "Found uiModel: $name - ${device.address} RSSI=$rssi")

            val exists = remoteDevices.value.any {
                it.bluetoothDevice.address == device.address
            }

            if (!exists) {
                val bleDevice = BleDevice(name, device.address)

                remoteDevices.value += RemoteDevice(
                    domainModel = bleDevice,
                    bluetoothDevice = device
                )
            }

        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with code $errorCode")
            setScanState(BleScanState.Stopped)
            // TODO: empty the scanned devices list
        }

    }

    private val gattCallback = object : BluetoothGattCallback() {



        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {

            Log.d(
                TAG, "STATE : " + when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> "Connected"
                    BluetoothProfile.STATE_DISCONNECTED -> "Disconnected"
                    BluetoothProfile.STATE_CONNECTING -> "Connecting"
                    BluetoothProfile.STATE_DISCONNECTING -> "Disconnecting"
                    else -> "UNKNOW"
                }
            )


            if (status != BluetoothGatt.GATT_SUCCESS) {
                val statusReason = gattStatusToString(status)
                Log.e(TAG, "Connection failed: $status ($statusReason)")

                remoteDevices.value = remoteDevices.value.map { remoteDevice ->
                    if (remoteDevice.bluetoothDevice.address == gatt.device.address) {
                        remoteDevice.copy(
                            // To reflect changes in UI
                            domainModel = remoteDevice.domainModel.copy(
                                isConnected = false,
                                isConnecting = false
                            ),
                            // To keep track in BleCentralServiceImp
                            isConnected = false,
                            isConnecting = false,
                        )
                    } else {
                        remoteDevice
                    }
                }.toSet()


                gatt.close()
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected")
                    gatt.discoverServices()


                    remoteDevices.value = remoteDevices.value.map { remoteDevice ->
                        if (remoteDevice.bluetoothDevice.address == gatt.device.address) {
                            remoteDevice.copy(
                                domainModel = remoteDevice.domainModel.copy(
                                    isConnected = true,
                                    isConnecting = false
                                ),
                                bluetoothDevice = gatt.device,
                                gatt = gatt,
                                isConnected = true,
                                isConnecting = false
                            )
                        } else {
                            remoteDevice
                        }
                    }.toSet()

                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected")

                    remoteDevices.value = remoteDevices.value.map { remoteDevice ->
                        if (remoteDevice.bluetoothDevice.address == gatt.device.address) {
                            remoteDevice.copy(
                                domainModel = remoteDevice.domainModel.copy(
                                    isConnected = false,
                                    isConnecting = false,
                                ),
                                bluetoothDevice = gatt.device,
                                gatt = gatt,
                                isConnected = false,
                                isConnecting = false,
                            )
                        } else {
                            remoteDevice
                        }
                    }.toSet()
                    gatt.close()
                }
            }
        }


        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")
                //findCharacteristics(gatt)
                logServices(gatt)
                updateUiModel(gatt)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val message = value.toString(Charsets.UTF_8)
            Log.d("BLE", "Received: $message")

            scope.launch {
                _incomingData.emit(value)
            }
        }


    }

    private fun updateUiModel(gatt: BluetoothGatt){

        remoteDevices.value = remoteDevices.value.map { remoteDevice ->
            if (remoteDevice.bluetoothDevice.address == gatt.device.address) {
                remoteDevice.copy(
                    domainModel = remoteDevice.domainModel.copy(
                        services = gatt.services.toDomain()
                    )
                )
            } else {
                remoteDevice
            }
        }.toSet()

    }

    override fun selectWriteableCharacteristic(
        device: BleDevice,
        bleCharacteristic: BleCharacteristic
    ) {
        remoteDevices.value.find { remoteDevice ->
            remoteDevice.bluetoothDevice.address == device.address
        }?.also { remoteDevice ->

            val service =
                remoteDevice.gatt?.getService(UUID.fromString(bleCharacteristic.serviceUuid))
            if (service == null) {
                Log.e(TAG, "Service with UUID ${bleCharacteristic.serviceUuid} not found")
                return
            }

            val characteristic = service.getCharacteristic(UUID.fromString(bleCharacteristic.uuid))
            if (characteristic == null) {
                Log.e(TAG, "Characteristic with UUID ${bleCharacteristic.uuid} not found")
                return
            }

            writeableCharacteristics.add(characteristic)

            remoteDevices.value = remoteDevices.value.map { remoteDevice ->
                if (remoteDevice.bluetoothDevice.address == device.address) {
                    remoteDevice.copy(
                        domainModel = remoteDevice.domainModel.copy(
                            services = device.services.map { service ->
                                service.copy(
                                    characteristics = service.characteristics.map { characteristic ->

                                        val isTargetCharacteristic = characteristic.uuid == bleCharacteristic.uuid &&
                                                service.uuid == bleCharacteristic.serviceUuid

                                        if(isTargetCharacteristic) {
                                            return@map characteristic.copy(
                                                selected = true
                                            )
                                        }

                                        characteristic
                                    }
                                )
                            }
                        )
                    )
                } else {
                    remoteDevice
                }
            }.toSet()
        }
    }

    override fun deselectWriteableCharacteristic(
        device: BleDevice,
        bleCharacteristic: BleCharacteristic
    ) {

        remoteDevices.value.find { remoteDevice ->
            remoteDevice.bluetoothDevice.address == device.address
        }?.also { remoteDevice ->

            val service =
                remoteDevice.gatt?.getService(UUID.fromString(bleCharacteristic.serviceUuid))
            if (service == null) {
                Log.e(TAG, "Service with UUID ${bleCharacteristic.serviceUuid} not found")
                return
            }

            val characteristic = service.getCharacteristic(UUID.fromString(bleCharacteristic.uuid))
            if (characteristic == null) {
                Log.e(TAG, "Characteristic with UUID ${bleCharacteristic.uuid} not found")
                return
            }

            if(!writeableCharacteristics.contains(characteristic))
                return

            writeableCharacteristics.remove(characteristic)

            remoteDevices.value = remoteDevices.value.map { remoteDevice ->
                if (remoteDevice.bluetoothDevice.address == device.address) {
                    remoteDevice.copy(
                        domainModel = remoteDevice.domainModel.copy(
                            services = device.services.map { service ->
                                service.copy(
                                    characteristics = service.characteristics.map { characteristic ->

                                        val isTargetCharacteristic =
                                            characteristic.uuid == bleCharacteristic.uuid &&
                                                    service.uuid == bleCharacteristic.serviceUuid

                                        if(isTargetCharacteristic){
                                            return@map characteristic.copy(
                                                selected = false
                                            )
                                        }

                                        characteristic
                                    }
                                )
                            }
                        )
                    )
                } else {
                    remoteDevice
                }
            }.toSet()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun enableNotification(
        device: BleDevice,
        bleCharacteristic: BleCharacteristic
    ) {
        remoteDevices.value.find { remoteDevice ->
            remoteDevice.bluetoothDevice.address == device.address
        }?.also { remoteDevice ->

            val service =
                remoteDevice.gatt?.getService(UUID.fromString(bleCharacteristic.serviceUuid))
            if (service == null) {
                Log.e(TAG, "Service with UUID ${bleCharacteristic.serviceUuid} not found")
                return
            }

            val characteristic = service.getCharacteristic(UUID.fromString(bleCharacteristic.uuid))
            if (characteristic == null) {
                Log.e(TAG, "Characteristic with UUID ${bleCharacteristic.uuid} not found")
                return
            }


            remoteDevice.gatt.also { gatt ->
                val enabledSuccess = enableNotificationOrIndication(gatt, characteristic)

                // Notification successfully enabled now we will mark the characteristic selected
                if (enabledSuccess) {

                     remoteDevices.value = remoteDevices.value.map { remoteDevice ->
                        if (remoteDevice.bluetoothDevice.address == device.address) {
                            remoteDevice.copy(
                                domainModel = remoteDevice.domainModel.copy(
                                    services = device.services.map { service ->
                                        service.copy(
                                            characteristics = service.characteristics.map { characteristic ->

                                                val isTargetCharacteristic = characteristic.uuid == bleCharacteristic.uuid &&
                                                        service.uuid == bleCharacteristic.serviceUuid

                                                if(isTargetCharacteristic) {
                                                    return@map characteristic.copy(
                                                        selected = true
                                                    )
                                                }

                                                characteristic
                                            }
                                        )
                                    }
                                )
                            )
                        } else {
                            remoteDevice
                        }
                    }.toSet()


                }

            }


        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    override fun disableNotification(
        device: BleDevice,
        bleCharacteristic: BleCharacteristic
    ) {

        remoteDevices.value.find { remoteDevice ->
            remoteDevice.bluetoothDevice.address == device.address
        }?.also { remoteDevice ->

            val service =
                remoteDevice.gatt?.getService(UUID.fromString(bleCharacteristic.serviceUuid))
            if (service == null) {
                Log.e(TAG, "Service with UUID ${bleCharacteristic.serviceUuid} not found")
                return
            }

            val characteristic = service.getCharacteristic(UUID.fromString(bleCharacteristic.uuid))
            if (characteristic == null) {
                Log.e(TAG, "Characteristic with UUID ${bleCharacteristic.uuid} not found")
                return
            }

            remoteDevice.gatt.also { gatt ->
                disableNotificationOrIndication(gatt, characteristic)
            }
        }


        remoteDevices.value = remoteDevices.value.map { remoteDevice ->
            if (remoteDevice.bluetoothDevice.address == device.address) {
                remoteDevice.copy(
                    domainModel = remoteDevice.domainModel.copy(
                        services = device.services.map { service ->
                            service.copy(
                                characteristics = service.characteristics.map { characteristic ->

                                    val isTargetCharacteristic =
                                        characteristic.uuid == bleCharacteristic.uuid &&
                                                service.uuid == bleCharacteristic.serviceUuid

                                    if(isTargetCharacteristic){
                                       return@map characteristic.copy(
                                            selected = false
                                        )
                                    }

                                    characteristic
                                }
                            )
                        }
                    )
                )
            } else {
                remoteDevice
            }
        }.toSet()

    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun logServices(gatt: BluetoothGatt){
        Log.d(TAG,"Found ${gatt.services.size} services in ${gatt.device.address} (${gatt.device.name})")
        gatt.services.forEach { service ->
            Log.d(TAG,"service UUID: ${service.uuid}")
            service.characteristics.forEach { characteristic ->
                Log.d(TAG," ----- characteristic: ${characteristic.uuid}")
                Log.d(
                    TAG,
                    " -------- properties: ${characteristic.getPropertyNames().joinToString(", ")}"
                )
                Log.d(
                    TAG,
                    " -------- permissions: ${characteristic.getPermissionNames().joinToString(", ")}"
                )

/*                // Check whether the characteristic supports notifications or indications.
                // properties is a bitmask, so we must use bitwise AND.
                val supportsNotify =
                    characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0

                val supportsIndicate =
                    characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0

                if(supportsNotify || supportsIndicate)
                    enableNotificationOrIndication(gatt, characteristic)*/

            }
        }
    }


    private fun BluetoothGattCharacteristic.getPropertyNames(): List<String> {
        val props = properties
        val result = mutableListOf<String>()

        if (props and BluetoothGattCharacteristic.PROPERTY_READ != 0)
            result.add("READ")

        if (props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0)
            result.add("WRITE")

        if (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)
            result.add("WRITE_NO_RESPONSE")

        if (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0)
            result.add("NOTIFY")

        if (props and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0)
            result.add("INDICATE")

        if (props and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE != 0)
            result.add("SIGNED_WRITE")

        if (props and BluetoothGattCharacteristic.PROPERTY_BROADCAST != 0)
            result.add("BROADCAST")

        return result
    }

    private fun BluetoothGattCharacteristic.getPermissionNames(): List<String> {
        val perms = permissions
        val result = mutableListOf<String>()

        if (perms and BluetoothGattCharacteristic.PERMISSION_READ != 0)
            result.add("READ")

        if (perms and BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED != 0)
            result.add("READ_ENCRYPTED")

        if (perms and BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM != 0)
            result.add("READ_ENCRYPTED_MITM")

        if (perms and BluetoothGattCharacteristic.PERMISSION_WRITE != 0)
            result.add("WRITE")

        if (perms and BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED != 0)
            result.add("WRITE_ENCRYPTED")

        if (perms and BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM != 0)
            result.add("WRITE_ENCRYPTED_MITM")

        if (perms and BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED != 0)
            result.add("WRITE_SIGNED")

        if (perms and BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM != 0)
            result.add("WRITE_SIGNED_MITM")

        return result
    }

    @Suppress("DEPRECATION")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun enableNotificationOrIndication(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ): Boolean {

        // Check whether the characteristic supports notifications or indications.
        // properties is a bitmask, so we must use bitwise AND.
        val supportsNotify =
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0

        val supportsIndicate =
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0

        // According to the BLE spec, a characteristic must support either NOTIFY or INDICATE
        // in order to enable updates. If neither is present, enabling notifications will fail.
        if (!supportsNotify and !supportsIndicate) {
            Log.e(TAG, "Characteristic ${characteristic.uuid} neither supports notifications nor indications")
            return false
        }


        // IMPORTANT:
        // setCharacteristicNotification() ONLY updates the local Android Bluetooth stack.
        // It does NOT enable notifications on the BLE peripheral.
        // A CCCD write is still REQUIRED to actually receive notifications/indications.
        gatt.setCharacteristicNotification(characteristic, true)
        
        // To enable notifications or indications, the client must write to the
        // Client Characteristic Configuration Descriptor (CCCD).
        // The following values can be written to the CCCD:
        //
        // 0x0001 (BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) — Enable notifications
        // 0x0002 (BluetoothGattDescriptor.ENABLE_INDICATION_VALUE) — Enable indications
        // 0x0000 (BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) — Disable both notifications and indications

        val cccd = characteristic.getDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        )


        // Per the Bluetooth spec, any characteristic that supports NOTIFY or INDICATE
        // must expose a CCCD. However, in practice, some BLE devices are misconfigured
        // or buggy and omit this descriptor.
        //
        // For this reason, getDescriptor() can return null even when NOTIFY is advertised.
        // Always null-check to avoid crashes and silent notification failures.
        if(cccd == null){
            Log.e(TAG, "Characteristic ${characteristic.uuid} does not have a CCCD")
            return false
        }

        // Choose correct value based on support
        // Prefer notifications over indications when both are supported
        val cccdValue = when{
            supportsNotify -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            supportsIndicate -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            else -> {
                return false
            }
        }


        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33+
                gatt.writeDescriptor(
                    cccd,
                    cccdValue
                )
            } else {
                // API < 33 (deprecated but required)
                cccd.value = cccdValue
                gatt.writeDescriptor(cccd)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        return true
    }

    @Suppress("DEPRECATION")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun disableNotificationOrIndication(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ){

        // Check whether the characteristic supports notifications or indications.
        // properties is a bitmask, so we must use bitwise AND.
        val supportsNotify =
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0

        val supportsIndicate =
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0

        if (!supportsNotify and !supportsIndicate) {
            Log.e(TAG, "Characteristic ${characteristic.uuid} neither supports notifications nor indications")
            return
        }

        // IMPORTANT:
        // Writing DISABLE_NOTIFICATION_VALUE stops updates on the peripheral,
        // but setCharacteristicNotification(false) is still required to
        // disable notification routing inside Android.
        gatt.setCharacteristicNotification(characteristic, false)

        val cccd = characteristic.getDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        )

        if(cccd == null){
            Log.e(TAG, "Characteristic ${characteristic.uuid} does not have a CCCD")
            return
        }

        val cccdValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33+
                gatt.writeDescriptor(
                    cccd,
                    cccdValue
                )
            } else {
                // API < 33 (deprecated but required)
                cccd.value = cccdValue
                gatt.writeDescriptor(cccd)
            }
        } catch (e: Exception){
            e.printStackTrace()
        }
    }



    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        scope.launch {
            remoteDevices.collect { remoteDevices ->
                Log.d(TAG, "Remote devices: $remoteDevices")

                _connectedDevices.value =
                    remoteDevices.filter { it.isConnected }.map { it.domainModel }.toSet()

                _scannedDevices.value =
                    remoteDevices.filter { !it.isConnected }.map { it.domainModel }.toSet()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val notificationBuilder =
            NotificationCompat.Builder(
                applicationContext,
                NOTIFICATION_CHANNEL_ID
            )
                .apply {
                    setSmallIcon(R.drawable.stat_notify_sync)
                    setContentTitle(NOTIFICATION_TITLE)
                    setContentText("Running")
                    setPriority(NotificationCompat.PRIORITY_HIGH)
                    setAutoCancel(false)
                    setOngoing(true)
                }


        val notification = notificationBuilder.build()

        startForeground(ON_GOING_NOTIFICATION_ID, notification)


        setServiceState(BleCentralServiceState.Running)

        return START_NOT_STICKY
    }



    override fun startService() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, BleCentralServiceImp::class.java)
        )
    }


    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override fun stopService() {
        scope.launch {
            stopScan()
            _scannedDevices.value = emptySet() // Never call this in stop scan as we want the scanned list to be available to the user even user stops the scan
            remoteDevices.value.map { it.gatt }.forEach { gatt ->
                gatt?.disconnect()
                gatt?.close()
            }
            setServiceState(BleCentralServiceState.Stopped)
            stopForeground()
            stopSelf()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun scanDevices() {
        remoteDevices.value = emptySet()

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        if(bluetoothLeScanner == null){
            Log.e(TAG, "BluetoothLeScanner not available (null)")
            return
        }

        scope.launch {
            bluetoothLeScanner?.startScan(scanCallback)
            setScanState(BleScanState.Started)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun stopScan() {

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        if(bluetoothLeScanner == null){
            Log.e(TAG, "BluetoothLeScanner not available (null)")
            return
        }


        scope.launch {
            bluetoothLeScanner?.stopScan(scanCallback)
            setScanState(BleScanState.Stopped)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun connectToDevice(device: BleDevice) {
        scope.launch {

            // 1. BluetoothProfile.STATE_CONNECTING is not reported in the GATT callback, so the state is updated here.
            // 2. Since the user initiates a connection by tapping a device in the scanned list, the "connecting" state
            //    should be shown only for items in the scanned devices list.

            _scannedDevices.value = scannedDevices.value.map { scannedDevice ->
                if (scannedDevice == device) {
                    scannedDevice.copy(
                        isConnecting = true
                    )
                } else {
                    scannedDevice
                }
            }.toSet()


            remoteDevices.value.find { it.domainModel.address == device.address }?.also { peripheralDevice ->
                peripheralDevice.bluetoothDevice.connectGatt(
                    this@BleCentralServiceImp,
                    false, // autoConnect = false (important)
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
            }

        }

    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun disconnectFromDevice(device: BleDevice) {
        scope.launch {
            remoteDevices.value.find{ it.domainModel == device }?.also { peripheralDevice ->
                peripheralDevice.gatt?.disconnect()
            }
        }
    }

    @Suppress("DEPRECATION")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun sendToConnectedDevices(data: ByteArray) {

        scope.launch {
            remoteDevices.value.filter { it.isConnected }.forEach { device ->
                writeableCharacteristics.forEach { characteristic ->
                    val deviceGatt = device.gatt

                    val properties = characteristic.properties

                    if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE == 0) {
                        Log.e(TAG, "Characteristic ${characteristic.uuid} does not support WRITE_NO_RESPONSE")
                        return@forEach
                    }


                    /**
                     * TODO:
                     * Currently we only support WRITE_TYPE_NO_RESPONSE.
                     *
                     * Support for WRITE_TYPE_DEFAULT (write with response) will be added later.
                     * WRITE_TYPE_DEFAULT requires confirmation from the remote device via
                     * onCharacteristicWrite().
                     *
                     * For WRITE_TYPE_DEFAULT we cannot continuously send data to the device.
                     * We need a queuing mechanism where:
                     *  - Data is sent to a device
                     *  - We wait for write confirmation from the remote device
                     *  - Only then we send the next queued packet
                     *
                     *  Sending multiple writes without waiting can cause:
                     *  - GATT busy errors
                     *  - Dropped writes
                     *  - Status 133 or silent failures
                     *
                     */

                    try {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            deviceGatt?.writeCharacteristic(
                                characteristic,
                                data,
                                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                            )
                        } else {
                            characteristic.value = data
                            characteristic.writeType =
                                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                            deviceGatt?.writeCharacteristic(characteristic)
                        }
                    }catch (e: Exception){
                        e.printStackTrace()
                    }
                }
            }
        }
    }


    private fun setServiceState(state: BleCentralServiceState){
        _serviceState.value = state
    }
    private fun setScanState(state: BleScanState){
        _scanState.value = state
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "createNotificationChannel()")
            val name: CharSequence = "Little Relay"
            val description = "Notifications from Little Relay Ble Central Service"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun gattStatusToString(status: Int): String =
        when (status) {
            BluetoothGatt.GATT_SUCCESS -> "GATT_SUCCESS"
            BluetoothGatt.GATT_READ_NOT_PERMITTED -> "GATT_READ_NOT_PERMITTED"
            BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "GATT_WRITE_NOT_PERMITTED"
            BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> "GATT_INSUFFICIENT_AUTHENTICATION"
            BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> "GATT_REQUEST_NOT_SUPPORTED"
            BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> "GATT_INSUFFICIENT_ENCRYPTION"
            BluetoothGatt.GATT_INVALID_OFFSET -> "GATT_INVALID_OFFSET"
            BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "GATT_INVALID_ATTRIBUTE_LENGTH"
            BluetoothGatt.GATT_CONNECTION_CONGESTED -> "GATT_CONNECTION_CONGESTED"
            BluetoothGatt.GATT_FAILURE -> "GATT_FAILURE"
            BluetoothGatt.GATT_CONNECTION_TIMEOUT -> "GATT_CONNECTION_TIMEOUT"
            133 -> "GATT_ERROR (133 – common Android BLE issue)"
            else -> "UNKNOWN_STATUS"
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
        val service: BleCentralServiceImp
            get() = this@BleCentralServiceImp // Return this instance of LocalService so clients can call public methods

    }

    companion object {
        private val TAG: String = BleCentralServiceImp::class.java.getSimpleName()
        const val NOTIFICATION_CHANNEL_ID = "BLE Central Service Notification"

        // cannot be zero
        const val ON_GOING_NOTIFICATION_ID = 519
        const val NOTIFICATION_TITLE = "BLE Central Service"
    }
}