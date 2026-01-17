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
package app.umerfarooq.littlerelay.ui.screens.dashboard

import app.umerfarooq.littlerelay.domain.model.BleDevice
import app.umerfarooq.littlerelay.domain.model.BleRole
import app.umerfarooq.littlerelay.domain.service.bluetooth.AdvertiseState
import app.umerfarooq.littlerelay.domain.service.bluetooth.BleCentralServiceState
import app.umerfarooq.littlerelay.domain.service.bluetooth.BlePeripheralServiceState
import app.umerfarooq.littlerelay.domain.service.bluetooth.BleScanState
import app.umerfarooq.littlerelay.domain.service.bluetooth.GattServerState
import app.umerfarooq.littlerelay.domain.service.bridge.BleToMqttBridgeServiceState
import app.umerfarooq.littlerelay.domain.service.mqtt.MqttConnectionState
import app.umerfarooq.littlerelay.domain.service.mqtt.MqttServiceState
import app.umerfarooq.littlerelay.domain.util.BluetoothState

data class DashboardState(
    val mqttConnectionState: MqttConnectionState = MqttConnectionState.Disconnected,
    val mqttServiceState: MqttServiceState = MqttServiceState.Stopped,
    val blePeripheralServiceState: BlePeripheralServiceState = BlePeripheralServiceState.Stopped,
    val bleToMqttBridgeServiceState: BleToMqttBridgeServiceState = BleToMqttBridgeServiceState.Stopped,
    val gattServerState: GattServerState = GattServerState.Close,
    val advertiseState: AdvertiseState = AdvertiseState.Stopped,
    val connectedBleDevices: Set<BleDevice> = emptySet(),
    val bluetoothState: BluetoothState = BluetoothState(false),
    val areBluetoothPermissionsGranted: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val bleCentralServiceState: BleCentralServiceState = BleCentralServiceState.Stopped,
    val scannedBleDevices: Set<BleDevice> = emptySet(),
    val scanState: BleScanState = BleScanState.Stopped,
    val bleRole: BleRole = BleRole.CENTRAL
)
