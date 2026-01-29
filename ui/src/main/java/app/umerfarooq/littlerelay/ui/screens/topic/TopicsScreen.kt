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
package app.umerfarooq.littlerelay.ui.screens.topic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults.itemShape
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.umerfarooq.littlerelay.domain.model.Topic
import app.umerfarooq.littlerelay.domain.model.TopicFilter
import app.umerfarooq.littlerelay.domain.model.TopicPublish
import app.umerfarooq.littlerelay.ui.screens.topic.component.TopicItem
import app.umerfarooq.littlerelay.ui.theme.LittleRelayTheme
import kotlinx.coroutines.launch

@Composable
fun TopicScreen(
    viewModel: TopicScreenViewModel = hiltViewModel(),
    onBackClick: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsState()

    TopicScreen(
        state = state,
        onEvent = { event ->

            if(event is TopicScreenEvent.OnBackClick) {
                onBackClick?.invoke()
            }

            viewModel.onEvent(event)

        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicScreen(
    state: TopicScreenState,
    onEvent: (TopicScreenEvent) -> Unit,
) {
    var showNewItemSheet by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Topics") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onEvent(TopicScreenEvent.OnBackClick)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {}
            ){
                IconButton(
                    onClick = { showNewItemSheet = true}
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add"
                    )
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {


            val tabs = listOf("Topic Filters", "Topic Publish")

            SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(text = title) }
                    )
                }
            }
            Box(
                modifier = Modifier.weight(1f)
            ) {
                when (selectedTabIndex) {
                    0 -> {

                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            state.topicFilters.forEach { topicFilter ->
                                TopicItem(
                                    topic = topicFilter,
                                    onDeleteClick = {
                                        onEvent(TopicScreenEvent.OnDeleteTopicClick(it as TopicFilter))
                                    },
                                    onUpdateClick = {
                                        onEvent(TopicScreenEvent.OnUpdateTopicClick(it as TopicFilter))
                                    }
                                )
                            }
                        }

                    }

                    1 -> {

                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            state.topicPublishes.forEach { topicPublish ->
                                TopicItem(
                                    topic = topicPublish,
                                    onDeleteClick = {
                                        onEvent(TopicScreenEvent.OnDeleteTopicClick(it as TopicPublish))
                                    },
                                    onUpdateClick = {
                                        onEvent(TopicScreenEvent.OnUpdateTopicClick(it as TopicPublish))
                                    }
                                )
                            }
                        }

                    }
                }
            }

        }
    }

    if(showNewItemSheet){

        ModalBottomSheet(
            onDismissRequest = { showNewItemSheet = false }
        ) {

            var topic by remember {
                mutableStateOf<Topic>(
                    if (selectedTabIndex == 0) {
                        TopicFilter(
                            topic = "some/topic",
                            qos = 0
                        )
                    } else {
                        TopicPublish(
                            topic = "some/topic",
                            qos = 0
                        )
                    }
                )
            }
            Surface {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {


                    Text(
                        when (topic) {
                            is TopicFilter -> "Add new Topic Filter"
                            is TopicPublish -> "Add new Topic to publish to"
                        }
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = topic.topic,
                        isError = topic.topic.isEmpty(),
                        onValueChange = {
                            topic = when (topic) {
                                is TopicFilter -> (topic as TopicFilter).copy(topic = it)
                                is TopicPublish -> (topic as TopicPublish).copy(topic = it)
                            }
                        },
                        shape = RoundedCornerShape(30.dp),
                        supportingText = {
                            if (topic.topic.isEmpty()) {
                                Text("Topic cannot be empty")
                            }
                        },
                        singleLine = true

                    )

                    SingleChoiceSegmentedButtonRow {
                        val qosOptions = listOf(0, 1, 2)
                        qosOptions.forEachIndexed { index, qosValue ->
                            SegmentedButton(
                                selected = topic.qos == qosValue,
                                onClick = {
                                    topic = when (topic) {
                                        is TopicFilter -> (topic as TopicFilter).copy(qos = qosValue)
                                        is TopicPublish -> (topic as TopicPublish).copy(qos = qosValue)
                                    }
                                },
                                shape = itemShape(index = index, count = qosOptions.size)
                            ) {
                                Text(qosValue.toString())
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    TextButton(
                        onClick = onClick@{

                            when (topic) {
                                is TopicFilter -> {
                                    if (state.topicFilters.has(topic as TopicFilter)) {
                                        scope.launch {
                                            showNewItemSheet = false
                                            snackbarHostState.showSnackbar("Topic filter already exists")

                                        }
                                        return@onClick
                                    }
                                }

                                is TopicPublish -> {
                                    if (state.topicPublishes.has(topic as TopicPublish)) {
                                        scope.launch {
                                            showNewItemSheet = false
                                            snackbarHostState.showSnackbar("Topic already exists")
                                        }
                                        return@onClick
                                    }
                                }
                            }


                            onEvent(TopicScreenEvent.OnAddNewTopic(topic))
                            showNewItemSheet = false
                        },
                        enabled = !topic.topic.isEmpty()
                    ) { Text("Add") }
                }
            }
        }

    }
}

private fun List<TopicFilter>.has(topic: TopicFilter): Boolean {
    return this.map { it.copy(id = 0) }.contains(topic.copy(id = 0))
}

private fun List<TopicPublish>.has(topic: TopicPublish): Boolean {
    return this.map { it.copy(id = 0) }.contains(topic.copy(id = 0))
}

@Preview
@Composable
private fun Preview() {
    LittleRelayTheme {
        TopicScreen(
            state = TopicScreenState(
                topicFilters = listOf(
                    TopicFilter(
                        topic = "test",
                        qos = 0
                    ),
                    TopicFilter(
                        topic = "test2",
                        qos = 0
                    )
                ),
                topicPublishes = listOf(
                    TopicPublish(
                        topic = "abc",
                        qos = 0
                    ),
                    TopicPublish(
                        topic = "def",
                        qos = 0
                    )
                )
            ),
            onEvent = {}
        )
    }
}