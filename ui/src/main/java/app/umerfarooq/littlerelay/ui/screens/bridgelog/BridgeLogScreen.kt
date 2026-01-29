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
package app.umerfarooq.littlerelay.ui.screens.bridgelog

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.umerfarooq.littlerelay.ui.screens.bridgelog.component.BridgeLogItem
import app.umerfarooq.littlerelay.ui.theme.LittleRelayTheme

@Composable
fun BridgeLogScreen(
    viewModel: BridgeLogScreenViewModel = hiltViewModel(),
    onBackClick: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BridgeLogScreen(
        state = state,
        onEvent = { event ->

            if(event is BridgeLogScreenEvent.OnBackClick)
                onBackClick?.invoke()

            viewModel.onEvent(event)

        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BridgeLogScreen(
    state: BridgeLogScreenState,
    onEvent: (BridgeLogScreenEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Bridge Logs")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onEvent(BridgeLogScreenEvent.OnBackClick)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onEvent(BridgeLogScreenEvent.OnClearLogsClick)
                        }
                    ){
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Clear Logs",
                        )
                    }
                    AssistChip(
                        leadingIcon = {
                            Icon(
                                imageVector = if(state.autoScroll) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onEvent(BridgeLogScreenEvent.OnAutoScrollClick)
                        },
                        label = { Text("Auto Scroll") },
                    )
                }
            )
        }
    ) { innerPadding ->

        val listState = rememberLazyListState()

        LaunchedEffect(key1 = state.autoScroll, key2 = state.bridgeLogs.size) {
            if(state.autoScroll){
                listState.scrollToItem(state.bridgeLogs.lastIndex)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = listState
        ) {
            state.bridgeLogs.forEach { log ->
                item {
                    BridgeLogItem(
                        modifier = Modifier.padding(vertical = 5.dp, horizontal = 8.dp),
                        log = log
                    )
                }
            }

        }
    }

}

@Preview
@Composable
private fun Preview() {
    LittleRelayTheme {
        BridgeLogScreen(
            state = BridgeLogScreenState(),
            onEvent = {}
        )
    }
}