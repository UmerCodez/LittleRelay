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

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.umerfarooq.littlerelay.domain.model.BleRole
import app.umerfarooq.littlerelay.domain.service.bluetooth.BleCentralServiceState
import app.umerfarooq.littlerelay.domain.service.bluetooth.BlePeripheralServiceState
import app.umerfarooq.littlerelay.domain.service.bluetooth.BleScanState
import app.umerfarooq.littlerelay.domain.service.bluetooth.GattServerState
import app.umerfarooq.littlerelay.domain.service.bridge.BleToMqttBridgeServiceState
import app.umerfarooq.littlerelay.domain.service.mqtt.MqttConnectionState
import app.umerfarooq.littlerelay.domain.service.mqtt.MqttServiceState
import app.umerfarooq.littlerelay.ui.component.InfoCard
import app.umerfarooq.littlerelay.ui.component.WarningCard
import app.umerfarooq.littlerelay.ui.screens.dashboard.components.AdvertiseStateView
import app.umerfarooq.littlerelay.ui.screens.dashboard.components.BleDeviceItem
import app.umerfarooq.littlerelay.ui.screens.dashboard.components.BleScanStateView
import app.umerfarooq.littlerelay.ui.screens.dashboard.components.CheckListItem
import app.umerfarooq.littlerelay.ui.screens.dashboard.components.GattServerStateView
import app.umerfarooq.littlerelay.ui.screens.dashboard.components.MqttConnectionStateView
import app.umerfarooq.littlerelay.ui.screens.dashboard.components.VerticalStep
import app.umerfarooq.littlerelay.ui.theme.LittleRelayTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DashBoard(
    viewModel: DashboardViewModel = hiltViewModel(),
    onMqttSettingsClick: (() -> Unit)? = null,
    onBleSettingsClick: (() -> Unit)? = null,
    onDeviceClick: ((address: String) -> Unit)? = null,
    onBridgeLogsClick: (() -> Unit)? = null,
    onAboutClick: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()


    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val bluetoothPermissionsState =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // In Android 12+, Bluetooth permissions (BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE, BLUETOOTH_SCAN)
            // are displayed to users as a single "Nearby Devices" permission in the system UI.
            // When requesting these permissions, users see only one permission dialog
            // regardless of how many individual runtime Bluetooth permissions app needs.
            rememberMultiplePermissionsState(
                listOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        } else {
            null
        }

    LaunchedEffect(
        bluetoothPermissionsState?.permissions?.map { it.status },
    ) {
        viewModel.onPermissionsStateChange()
    }

    LaunchedEffect(locationPermissionState.status) {
        viewModel.onPermissionsStateChange()
    }

    LaunchedEffect(Unit) {
        notificationPermissionState?.launchPermissionRequest()
    }

    DashBoard(
        state = uiState,
        onEvent = { event ->

            when (event) {

                is DashboardEvent.OnGrantBluetoothPermissionClick -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        bluetoothPermissionsState?.launchMultiplePermissionRequest()
                    }
                }

                is DashboardEvent.OnGrantLocationPermissionClick -> {
                    locationPermissionState.launchPermissionRequest()
                }

                is DashboardEvent.OnStartBlePeripheralServiceClick -> {
                    bluetoothPermissionsState?.launchMultiplePermissionRequest()
                }

                is DashboardEvent.OnNavigateToMqttSettingsClick -> {
                    onMqttSettingsClick?.invoke()
                }

                is DashboardEvent.OnNavigateToBLeSettingClick -> {
                    onBleSettingsClick?.invoke()
                }

                is DashboardEvent.OnViewDeviceServicesClick -> {
                    onDeviceClick?.invoke(event.bleDevice.address)
                }

                is DashboardEvent.OnBridgeLogsClick -> {
                    onBridgeLogsClick?.invoke()
                }

                is DashboardEvent.OnAboutClick -> {
                    onAboutClick?.invoke()
                }

                else -> {}
            }

            viewModel.onEvent(event)

        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashBoard(
    isAndroid12OrLater: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    state: DashboardState,
    onEvent: (DashboardEvent) -> Unit,
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    Icon(
                        modifier = Modifier.padding(start = 8.dp),
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = null
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            onEvent(DashboardEvent.OnAboutClick)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(10.dp)
                .animateContentSize()
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(5.dp)
        ) {

            VerticalStep(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null
                    )
                },
                label = {
                    Text("MQTT Service")
                },
                actions = {
                    IconButton(
                        enabled = state.mqttServiceState is MqttServiceState.Stopped,
                        onClick = {
                            onEvent(DashboardEvent.OnStartMQTTServiceClick)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                    }

                    IconButton(
                        enabled = state.mqttServiceState is MqttServiceState.Running,
                        onClick = {
                            onEvent(DashboardEvent.OnStopMQTTServiceClick)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null
                        )
                    }

                    IconButton(
                        onClick = {
                            onEvent(DashboardEvent.OnNavigateToMqttSettingsClick)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null
                        )
                    }
                }
            ) {
                AnimatedVisibility(state.mqttServiceState is MqttServiceState.Running) {
                    MqttConnectionStateView(
                        state = state.mqttConnectionState,
                        onConnectClick = {
                            onEvent(DashboardEvent.OnMqttConnectClick)
                        },
                        onDisconnectClick = {
                            onEvent(DashboardEvent.OnMqttDisconnectClick)
                        }
                    )
                }

            }

            VerticalStep(
                icon = {
                    Icon(
                        imageVector = if(state.bluetoothState.isEnable) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                        contentDescription = null
                    )
                },
                label = {
                    Text("Bluetooth")
                }
            ) {
                Column {
                    AnimatedVisibility(!state.bluetoothState.isEnable) {
                        WarningCard("Bluetooth is not enabled")
                    }
                }
            }

            AnimatedVisibility(isAndroid12OrLater && !state.areBluetoothPermissionsGranted){
                VerticalStep(
                    icon = {
                        Icon(
                            imageVector = if(state.areBluetoothPermissionsGranted) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("Bluetooth Permissions")
                    },
                    actions = {
                        TextButton(
                            enabled = !state.areBluetoothPermissionsGranted,
                            onClick = {
                                onEvent(DashboardEvent.OnGrantBluetoothPermissionClick)
                            }
                        ) {
                            Text("GRANT")
                        }
                    }
                ) {
                    WarningCard("Permission not granted")
                }
            }

            // For Android 11 and lower
            AnimatedVisibility( !isAndroid12OrLater && state.bleRole == BleRole.CENTRAL){
                VerticalStep(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("Location Permission")
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                onEvent(DashboardEvent.OnGrantLocationPermissionClick)
                            }
                        ) {
                            Text("GRANT")
                        }
                    }
                ) {

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        var showExplanation by remember { mutableStateOf(false) }

                        TextButton(
                            onClick = {
                                showExplanation = !showExplanation
                            }
                        ) {
                            Text("Why?")
                        }

                        AnimatedVisibility(showExplanation) {
                            InfoCard(
                                modifier = Modifier.clickable{
                                    showExplanation = false
                                },
                                infoMessage = "To find and connect to nearby Bluetooth devices, Android 11 or lower devices requires Location permissions to be granted. This is a standard Android requirement for Bluetooth Low Energy (BLE) scanning."
                            )
                        }

                        AnimatedVisibility(!state.locationPermissionGranted) {
                            WarningCard("Permission not granted")
                        }
                    }
                }
            }

            AnimatedVisibility(state.bleRole == BleRole.PERIPHERAL) {
                VerticalStep(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text("BLE Service (Peripheral)")
                    },
                    actions = {

                        IconButton(
                            enabled = state.blePeripheralServiceState is BlePeripheralServiceState.Stopped,
                            onClick = {
                                onEvent(DashboardEvent.OnStartBlePeripheralServiceClick)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                        }

                        IconButton(
                            enabled = state.blePeripheralServiceState is BlePeripheralServiceState.Running,
                            onClick = {
                                onEvent(DashboardEvent.OnStopBlePeripheralServiceClick)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null
                            )
                        }

                        IconButton(
                            onClick = {
                                onEvent(DashboardEvent.OnNavigateToBLeSettingClick)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null
                            )
                        }

                    }
                ) {
                    Column {

                        AnimatedVisibility(state.blePeripheralServiceState is BlePeripheralServiceState.Running) {
                            GattServerStateView(
                                state = state.gattServerState,
                                onOpenClick = {
                                    onEvent(DashboardEvent.OnOpenGATTServerClick)
                                },
                                onCloseClick = {
                                    onEvent(DashboardEvent.OnCloseGATTServerClick)
                                }
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        AnimatedVisibility(state.gattServerState is GattServerState.Open) {

                            AdvertiseStateView(
                                state = state.advertiseState,
                                onStartClick = {
                                    onEvent(DashboardEvent.OnStartAdvertisingClick)
                                },
                                onStopClick = {
                                    onEvent(DashboardEvent.OnStopAdvertisingClick)
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(state.bleRole == BleRole.CENTRAL) {

                Column {

                    VerticalStep(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.MiscellaneousServices,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text("BLE Service (Central)")
                        },
                        actions = {
                            IconButton(
                                enabled = state.bleCentralServiceState is BleCentralServiceState.Stopped,
                                onClick = {
                                    onEvent(DashboardEvent.OnStartBleCentralServiceClick)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                            }

                            IconButton(
                                enabled = state.bleCentralServiceState is BleCentralServiceState.Running,
                                onClick = {
                                    onEvent(DashboardEvent.OnStopBleCentralServiceClick)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = null
                                )
                            }

                            IconButton(
                                onClick = {
                                    onEvent(DashboardEvent.OnNavigateToBLeSettingClick)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null
                                )
                            }
                        }
                    ) {

                        AnimatedVisibility(state.bleCentralServiceState is BleCentralServiceState.Running) {

                            // If android 11 or lower and location permission not granted
                            val shouldShowBleScanControls = !isAndroid12OrLater && state.locationPermissionGranted || isAndroid12OrLater && state.areBluetoothPermissionsGranted

                            if(shouldShowBleScanControls) {
                                BleScanStateView(
                                    state = state.scanState,
                                    onStartClick = {
                                        onEvent(DashboardEvent.OnStartScanClick)
                                    },
                                    onStopClick = {
                                        onEvent(DashboardEvent.OnStopScanClick)
                                    }
                                )
                            } else {
                                if(!isAndroid12OrLater) {
                                    WarningCard("Location permission not granted")
                                } else {
                                    WarningCard("Bluetooth permissions not granted")
                                }
                            }

                        }

                    }

                    VerticalStep(
                        icon = {
                            if (state.scanState is BleScanState.Started) {
                                CircularProgressIndicator()
                            } else
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null
                                )
                        },
                        label = {
                            Text("Scanned Devices")
                        }
                    ) {
                        AnimatedVisibility(state.scannedBleDevices.isNotEmpty()) {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            ) {
                                state.scannedBleDevices.forEach { device ->

                                    BleDeviceItem(
                                        device = device,
                                        onConnectClick = {
                                            onEvent(DashboardEvent.OnConnectClick(it))
                                        },
                                        onDisconnectClick = {
                                            onEvent(DashboardEvent.OnDisconnectClick(it))
                                        }
                                    )

                                }
                            }
                        }
                    }

                }
            }

            VerticalStep(
                icon = {
                    Icon(
                        imageVector = Icons.Default.BluetoothConnected,
                        contentDescription = null
                    )
                },
                label = {
                    Text("Connected Devices")
                }
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ){

                    if (state.connectedBleDevices.isEmpty()) {
                        WarningCard("No Device Connected")
                    }


                    state.connectedBleDevices.forEach { device ->

                        BleDeviceItem(
                            device = device,
                            onConnectClick = {
                                onEvent(DashboardEvent.OnConnectClick(it))
                            },
                            onDisconnectClick = {
                                onEvent(DashboardEvent.OnDisconnectClick(it))
                            },
                            showExploreServicesButton = state.bleRole == BleRole.CENTRAL,
                            onExploreServicesClick = {
                                onEvent(DashboardEvent.OnViewDeviceServicesClick(it))
                            }
                        )

                    }
                }
            }

            VerticalStep(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null
                    )
                },
                label = {
                    Text("BLE <-> MQTT Service")
                },
                actions = {
                    Card(
                        modifier = Modifier.padding(end = 8.dp),
                        elevation = CardDefaults.elevatedCardElevation(
                            defaultElevation = 5.dp
                        )
                    ) {
                        Text(
                            modifier = Modifier.padding(5.dp),
                            text = "Auto"
                        )
                    }
                }
            ) {

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    AnimatedVisibility(state.bleToMqttBridgeServiceState !is BleToMqttBridgeServiceState.Running) {
                        InfoCard("This service starts automatically")
                    }

                    AnimatedVisibility(state.bleToMqttBridgeServiceState is BleToMqttBridgeServiceState.Running) {
                        InfoCard("Bridge Service Running")
                    }

                    TextButton(
                        onClick = {
                            onEvent(DashboardEvent.OnBridgeLogsClick)
                        }
                    ) {
                        Text("View Logs")
                    }

                    CheckListItem(
                        checked = state.mqttServiceState is MqttServiceState.Running,
                        label = {
                            Text(
                                text = "MQTT Service Running",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                    CheckListItem(
                        checked = state.mqttConnectionState is MqttConnectionState.Connected,
                        label = {
                            Text(
                                text = "Connected To Broker",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )

                    CheckListItem(
                        checked = state.blePeripheralServiceState is BlePeripheralServiceState.Running || state.bleCentralServiceState is BleCentralServiceState.Running,
                        label = {
                            Text(
                                text = "BLE Service Running",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )

                    CheckListItem(
                        checked = state.connectedBleDevices.isNotEmpty(),
                        label = {
                            Text(
                                text = "BLE Devices connected",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                }

            }

        }
    }


}



@Preview
@Composable
private fun HomeScreenPreview(modifier: Modifier = Modifier) {

    LittleRelayTheme {
        DashBoard(
            isAndroid12OrLater = false,
            state = DashboardState(
                mqttConnectionState = MqttConnectionState.Connected,
                gattServerState = GattServerState.Open,
            ),
            onEvent = {}
        )
    }
}