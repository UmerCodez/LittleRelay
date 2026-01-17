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

sealed interface MqttConfigScreenEvent{
    data class OnBrokerAddressChange(val address: String) : MqttConfigScreenEvent
    data class OnBrokerPortChange(val brokerPort: Int) : MqttConfigScreenEvent
    data class OnUseSSLChange(val useSSL: Boolean) : MqttConfigScreenEvent
    data class OnUseWebsocketChange(val useWebsocket: Boolean) : MqttConfigScreenEvent
    data class OnUseCredentialsChange(val useCredentials: Boolean) : MqttConfigScreenEvent
    data class OnConnectionTimeoutChange(val connectionTimeout: Int) : MqttConfigScreenEvent
    data class OnPasswordChange(val password: String) : MqttConfigScreenEvent
    data class OnUserNameChange(val userName: String) : MqttConfigScreenEvent
    data object OnBackClick: MqttConfigScreenEvent
    data object OnManageTopicsClick: MqttConfigScreenEvent
}