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
package app.umerfarooq.littlerelay.ui.screens.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import app.umerfarooq.littlerelay.ui.screens.about.AboutScreen
import app.umerfarooq.littlerelay.ui.screens.bleconfig.BleConfigScreen
import app.umerfarooq.littlerelay.ui.screens.bridgelog.BridgeLogScreen
import app.umerfarooq.littlerelay.ui.screens.dashboard.DashBoard
import app.umerfarooq.littlerelay.ui.screens.deviceservices.DeviceServicesScreen
import app.umerfarooq.littlerelay.ui.screens.mqttconfig.MqttConfigScreen
import app.umerfarooq.littlerelay.ui.screens.topic.TopicScreen

sealed interface NavKey {
    data object MqttConfigScreen : NavKey
    data object HomeScreen : NavKey
    data object TopicScreen : NavKey
    data object BleConfigScreen : NavKey
    data object BridgeLogScreen: NavKey
    data class DeviceServicesScreen(val deviceAddress: String) : NavKey
    data object AboutScreen: NavKey
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavScreen(modifier: Modifier = Modifier) {

    val backStack = remember { mutableStateListOf<NavKey>(NavKey.HomeScreen) }
    NavDisplay(
        modifier = Modifier
            .fillMaxSize(),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {

            entry<NavKey.MqttConfigScreen> {
                MqttConfigScreen(
                    onBackClick = {
                        backStack.removeLastOrNull()
                    },
                    onManageTopicsClick = {
                        backStack.add(NavKey.TopicScreen)
                    }
                )
            }
            entry<NavKey.HomeScreen> {
                DashBoard(
                    onMqttSettingsClick = {
                        backStack.add(NavKey.MqttConfigScreen)
                    },
                    onBleSettingsClick = {
                        backStack.add(NavKey.BleConfigScreen)
                    },
                    onDeviceClick = { deviceAddress ->
                        backStack.add(NavKey.DeviceServicesScreen(deviceAddress))
                    },
                    onBridgeLogsClick = {
                        backStack.add(NavKey.BridgeLogScreen)
                    },
                    onAboutClick = {
                        backStack.add(NavKey.AboutScreen)
                    }
                )
            }
            entry<NavKey.TopicScreen> {
                TopicScreen(
                    onBackClick = {
                        backStack.removeLastOrNull()
                    }
                )
            }
            entry<NavKey.BleConfigScreen> {
                BleConfigScreen(
                    onBackClick = {
                        backStack.removeLastOrNull()
                    }
                )
            }
            entry<NavKey.DeviceServicesScreen> { deviceServicesScreen ->
                DeviceServicesScreen(deviceAddress = deviceServicesScreen.deviceAddress)
            }
            entry<NavKey.BridgeLogScreen> {
                BridgeLogScreen(
                    onBackClick = {
                        backStack.removeLastOrNull()
                    }
                )
            }

            entry<NavKey.AboutScreen> {
                AboutScreen(
                    onBackClick = {
                        backStack.removeLastOrNull()
                    }
                )
            }
        }
    )

}
