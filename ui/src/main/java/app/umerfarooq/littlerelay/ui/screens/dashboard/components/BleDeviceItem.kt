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
package app.umerfarooq.littlerelay.ui.screens.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.umerfarooq.littlerelay.domain.model.BleDevice
import app.umerfarooq.littlerelay.ui.theme.LittleRelayTheme

@Composable
fun BleDeviceItem(
    device: BleDevice,
    modifier: Modifier = Modifier,
    onConnectClick: ((BleDevice) -> Unit)? = null,
    onDisconnectClick: ((BleDevice) -> Unit)? = null,
    showExploreServicesButton: Boolean = false,
    onExploreServicesClick: ((BleDevice) -> Unit)? = null
) {


    ListItem(
        modifier = modifier,
        headlineContent = {
            Text(device.name)
        },
        supportingContent = {
            Column {
                Text(device.address)
                if(showExploreServicesButton){
                    TextButton(
                        onClick = {
                            onExploreServicesClick?.invoke(device)
                        }
                    ) {
                        Text(
                            text = "Explore Services"
                        )
                    }
                }
            }
        },
        overlineContent = {
            Column {
                if (device.isConnecting) {
                    LinearProgressIndicator()
                }
            }
        },
        leadingContent = {
            Icon(
                imageVector = if(device.isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                contentDescription = null
            )
        },
        trailingContent = {
            if (!device.isConnected) {
                TextButton(
                    enabled = !device.isConnecting,
                    onClick = {
                        onConnectClick?.invoke(device)
                    }
                ) {
                    Text("Connect")
                }
            } else {
                TextButton(
                    onClick = {
                        onDisconnectClick?.invoke(device)
                    }
                ) {
                    Text("Disconnect")
                }
            }
        }
    )

}

@Preview
@Composable
private fun Preview() {
    LittleRelayTheme {
        BleDeviceItem(
            device = BleDevice(
                name = "Some Device",
                address = "00:11:22:33:44:55"
            ),
            onExploreServicesClick = {}
        )

    }
}
