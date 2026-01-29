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
package app.umerfarooq.littlerelay.ui.screens.bleconfig

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SettingsBluetooth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.umerfarooq.littlerelay.domain.model.AdvertiseMode
import app.umerfarooq.littlerelay.domain.model.AdvertiseTxPower
import app.umerfarooq.littlerelay.domain.model.BleRole

@Composable
fun BleConfigScreen(
    viewModel: BleConfigScreenViewModel = hiltViewModel(),
    onBackClick: (() -> Unit)? = null
) {

    val state by viewModel.state.collectAsState()

    BleConfigScreen(
        state = state,
        onEvent = { event ->

            if(event is BleConfigScreenEvent.OnBackClick)
                onBackClick?.invoke()

            viewModel.onEvent(event)
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleConfigScreen(
    state: BleConfigScreenState,
    onEvent: (BleConfigScreenEvent) -> Unit,
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE Config") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onEvent(BleConfigScreenEvent.OnBackClick)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            ListItem(
                headlineContent = {
                    Text("Role")
                },
                supportingContent = {
                    Text(state.bleConfig.bleRole.name)
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.SettingsBluetooth,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            expanded = !expanded
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null
                        )
                    }


                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        }
                    ) {
                        BleRole.entries.forEach {
                            DropdownMenuItem(
                                text = { Text(it.name) },
                                onClick = {
                                    onEvent(BleConfigScreenEvent.OnBleRoleChange(it))
                                    expanded = false
                                }
                            )
                        }
                    }
                }

            )
            AnimatedVisibility(state.bleConfig.bleRole == BleRole.PERIPHERAL) {
                Column{
                    ListItem(
                        headlineContent = {
                            Text("Tx power")
                        },
                        supportingContent = {
                            Text(state.bleConfig.bleAdvertiseSetting.txPowerLevel.name)
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.SettingsBluetooth,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            var expanded by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = {
                                    expanded = !expanded
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null
                                )
                            }


                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = {
                                    expanded = false
                                }
                            ) {
                                AdvertiseTxPower.entries.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.name) },
                                        onClick = {
                                            onEvent(BleConfigScreenEvent.OnTxPowerLevelChange(it))
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                    )
                    ListItem(
                        headlineContent = {
                            Text("Mode")
                        },
                        supportingContent = {
                            Text(state.bleConfig.bleAdvertiseSetting.mode.name)
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.SettingsBluetooth,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            var expanded by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = {
                                    expanded = !expanded
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null
                                )
                            }


                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = {
                                    expanded = false
                                }
                            ) {
                                AdvertiseMode.entries.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.name) },
                                        onClick = {
                                            onEvent(BleConfigScreenEvent.OnAdvertiseModeChange(it))
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                    )
                    ListItem(
                        headlineContent = {
                            Text("Include Device Name")
                        },
                        trailingContent = {
                            Switch(
                                checked = state.bleConfig.bleAdvertiseSetting.includeDeviceName,
                                onCheckedChange = {
                                    onEvent(BleConfigScreenEvent.OnIncludeDeviceNameChange(it))
                                }
                            )
                        }
                    )

                }
            }

        }
    }

}