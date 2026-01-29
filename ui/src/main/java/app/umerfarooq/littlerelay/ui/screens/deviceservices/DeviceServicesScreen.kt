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
package app.umerfarooq.littlerelay.ui.screens.deviceservices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.umerfarooq.littlerelay.domain.model.BleDevice
import app.umerfarooq.littlerelay.domain.model.fakeServices
import app.umerfarooq.littlerelay.ui.component.InfoCard
import app.umerfarooq.littlerelay.ui.component.WarningCard
import app.umerfarooq.littlerelay.ui.screens.deviceservices.component.ServiceItem
import app.umerfarooq.littlerelay.ui.theme.LittleRelayTheme

@Composable
fun DeviceServicesScreen(
    deviceAddress: String,
    viewModel: DeviceServicesScreenViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(deviceAddress) {
        viewModel.loadDevice(deviceAddress)
    }

    DeviceServicesScreen(
        state = state,
        onEvent = viewModel::onEvent
    )
    
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceServicesScreen(
    state: DeviceServicesScreenState,
    onEvent: (DeviceServicesScreenEvent) -> Unit
) {
    var showWarningSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = state.currentDevice?.name ?: "No Device")
                },
                actions = {
                    IconButton(
                        onClick = {
                            showWarningSheet = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        if(state.currentDevice == null){
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Device Not Connected",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Text(
                text = "BLE Services",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "${state.currentDevice.services.size} service${if (state.currentDevice.services.size != 1) "s" else ""} discovered",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.currentDevice.services, key = { it.uuid }) { service ->
                    ServiceItem(
                        service = service,
                        onNotifiableCharacteristicSelectionStateChange = { characteristic, selectionState ->
                            onEvent(
                                DeviceServicesScreenEvent.OnNotifiableCharacteristicSelectionStateChange(
                                    device = state.currentDevice,
                                    bleCharacteristic = characteristic,
                                    selected = selectionState
                                )
                            )
                        },
                        onWriteableCharacteristicSelectionStateChange = { characteristic, selectionState ->
                            onEvent(
                                DeviceServicesScreenEvent.OnWriteableCharacteristicSelectionStateChange(
                                    device = state.currentDevice,
                                    bleCharacteristic = characteristic,
                                    selected = selectionState
                                )
                            )
                        }

                    )
                }
            }
        }
    }

    if(showWarningSheet){
        ModalBottomSheet(
            onDismissRequest = { showWarningSheet = false },
        ) {
            SheetContent(state)
        }
    }
}

@Composable
private fun SheetContent(
    state: DeviceServicesScreenState,
    modifier: Modifier = Modifier
) {
    Surface{
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            val noWarning = state.hasNotifiableCharacteristic and state.hasWriteableCharacteristic and state.selectedNotifiableCharacteristics.isNotEmpty() and state.selectedWriteableCharacteristics.isNotEmpty()

            if(noWarning){
                InfoCard("No Warnings")
                return@Column
            }


            if(!state.hasNotifiableCharacteristic){
                WarningCard("This Device doesn't offer any characteristic which supports notifications/indications")
            }

            if(!state.hasWriteableCharacteristic){
                WarningCard("This Device doesn't offer any characteristic which is writeable (WRITE NO RESPOSE)")
            }

            if(state.selectedNotifiableCharacteristics.isEmpty()){
                WarningCard("No Notifiable characteristic selected")
            } else {
                InfoCard("Selected Notifiable Characteristics")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                ) {
                    state.selectedNotifiableCharacteristics.forEach { characteristic ->
                        Text(
                            modifier = Modifier.padding(8.dp),
                            text = characteristic.uuid,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            if(state.selectedWriteableCharacteristics.isEmpty()){
                WarningCard("No writeable characteristic selected")
            } else {
                InfoCard("Selected Notifiable Characteristics")
                state.selectedWriteableCharacteristics.forEach { characteristic ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            modifier = Modifier.padding(8.dp),
                            text = characteristic.uuid
                        )
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun BleServicesPreview() {
    LittleRelayTheme {
        DeviceServicesScreen(
            state = DeviceServicesScreenState(
                currentDevice = BleDevice(
                    name = "Device Name",
                    address = "Device Address",
                    isConnected = false,
                    services = fakeServices
                )
            ),
            onEvent = {}
        )
    }
}

@Preview
@Composable
fun SheetContentPreview(modifier: Modifier = Modifier) {
    LittleRelayTheme {
        SheetContent(
            DeviceServicesScreenState(
                currentDevice = BleDevice(
                    name = "Device Name",
                    address = "Device Address",
                    isConnected = false,
                    services = fakeServices
                )
            ),
        )
    }
}

