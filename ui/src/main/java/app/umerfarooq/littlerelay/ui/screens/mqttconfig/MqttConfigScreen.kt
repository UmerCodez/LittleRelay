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
package app.umerfarooq.littlerelay.ui.screens.mqttconfig

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person2
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.umerfarooq.littlerelay.ui.component.EditTextPref
import app.umerfarooq.littlerelay.ui.component.SwitchPref
import app.umerfarooq.littlerelay.ui.theme.LittleRelayTheme

@Composable
fun MqttConfigScreen(
    viewModel: MqttConfigScreenViewModel = hiltViewModel(),
    onBackClick: (() -> Unit)? = null,
    onManageTopicsClick: (() -> Unit)? = null
) {
    val state by viewModel.mqttConfigScreenState.collectAsState()

    MqttConfigScreen(
        state = state,
        onEvent = { event ->

            if(event is MqttConfigScreenEvent.OnBackClick) {
                onBackClick?.invoke()
            }
            else if(event is MqttConfigScreenEvent.OnManageTopicsClick) {
                onManageTopicsClick?.invoke()
            }


            viewModel.onEvent(event)
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MqttConfigScreen(
    state: MqttConfigScreenState,
    onEvent: (MqttConfigScreenEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("MQTT Settings")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onEvent(MqttConfigScreenEvent.OnBackClick)
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
    ){ innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(10.dp)
                .verticalScroll(rememberScrollState())
        ) {

            EditTextPref(
                value = state.mqttConfig.brokerAddress,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Broker Address"
                    )
                },
                title = {
                    Text(text = "Broker Address")
                },
                isError = {
                    it.isEmpty()
                },
                onUpdateClick = { updatedValue ->
                    onEvent(MqttConfigScreenEvent.OnBrokerAddressChange(updatedValue))
                }
            )

            EditTextPref(
                value = state.mqttConfig.brokerPort.toString(),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Numbers,
                        contentDescription = null
                    )
                },
                title = {
                    Text(text = "Broker Port")
                },
                isError = { newValue ->
                    newValue.isEmpty() || newValue.toIntOrNull() == null || newValue.toInt() !in 1..65535
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                onUpdateClick = { updatedValue ->
                    onEvent(MqttConfigScreenEvent.OnBrokerPortChange(updatedValue.toInt()))
                }
            )
            EditTextPref(
                value = state.mqttConfig.connectionTimeoutSecs.toString(),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Timelapse,
                        contentDescription = "Broker Address"
                    )
                },
                title = { Text("Connection Timeout") },
                isError = { value ->
                    value.isEmpty() || value.toIntOrNull() == null || value.toInt() !in 5..20
                },
                errorText = "Range: 5-20s",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                onUpdateClick = {
                    onEvent(MqttConfigScreenEvent.OnConnectionTimeoutChange(it.toInt()))
                },

                )

            ListItem(
                modifier = Modifier.clickable{
                    onEvent(MqttConfigScreenEvent.OnManageTopicsClick)
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null
                    )
                },
                headlineContent = {
                    Text("Manage topics")
                },
                trailingContent = {
                    IconButton(
                        onClick = {
                            onEvent(MqttConfigScreenEvent.OnManageTopicsClick)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null
                        )
                    }
                }
            )

            SwitchPref(
                checked = state.mqttConfig.useSSL,
                icon = {
                    Icon(
                        imageVector = Icons.Default.EnhancedEncryption,
                        contentDescription = "Broker Address"
                    )
                },
                title = {
                    Text("SSL")
                },
                onCheckedChange = {
                    onEvent(MqttConfigScreenEvent.OnUseSSLChange(it))
                }

            )

            //TODO: Add websocket option later

            SwitchPref(
                checked = state.mqttConfig.useCredentials,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Broker Address"
                    )
                },
                title = {
                    Text("Credentials")
                },
                onCheckedChange = {
                    onEvent(MqttConfigScreenEvent.OnUseCredentialsChange(it))
                }

            )

            AnimatedVisibility(state.mqttConfig.useCredentials) {
                Column {
                    EditTextPref(
                        value = state.mqttConfig.userName,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Person2,
                                contentDescription = null
                            )
                        },
                        title = { Text("Username") },
                        isError = { value ->
                            value.isEmpty()
                        },
                        onUpdateClick = {
                            onEvent(MqttConfigScreenEvent.OnUserNameChange(it))
                        }
                    )

                    EditTextPref(
                        value = state.mqttConfig.password,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Password,
                                contentDescription = null
                            )
                        },
                        title = { Text("Password") },
                        password = true,
                        isError = { value ->
                            value.isEmpty()
                        },
                        onUpdateClick = {
                            onEvent(MqttConfigScreenEvent.OnPasswordChange(it))
                        }
                    )
                }
            }
        }
    }

}

@Preview
@Composable
fun Preview() {
    LittleRelayTheme {
        MqttConfigScreen(
            state = MqttConfigScreenState(),
            onEvent = {}
        )
    }
}
