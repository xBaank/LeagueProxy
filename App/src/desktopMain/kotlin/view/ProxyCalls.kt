package view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import extensions.prettyPrint
import org.koin.compose.koinInject
import proxies.interceptors.*
import proxies.interceptors.Call.*
import proxies.interceptors.Call.ConfigCall.ConfigResponse
import proxies.interceptors.Call.RmsCall.RmsRequest
import proxies.interceptors.Call.RmsCall.RmsResponse
import proxies.interceptors.Call.RtmpCall.RtmpRequest
import proxies.interceptors.Call.RtmpCall.RtmpResponse
import proxies.interceptors.Call.XmppCall.XmppRequest
import proxies.interceptors.Call.XmppCall.XmppResponse
import proxies.utils.Amf0PrettyBuilder
import simpleJson.serialized
import simpleJson.serializedPretty


@Composable
fun ProxyCalls(isDarkColors: MutableState<Boolean>) {
    var searchText by remember { mutableStateOf("") }
    val items: SnapshotStateList<Call> = remember { mutableStateListOf() }
    val dropDownItems = mutableListOf("XMPP", "RTMP", "CONFIG", "RMS")
    val selectedItems = remember { mutableStateOf(dropDownItems.toList()) }
    val rtmpInterceptor = koinInject<RtmpProxyInterceptor>()
    val configInterceptor = koinInject<ConfigProxyInterceptor>()
    val xmppInterceptor = koinInject<XmppProxyInterceptor>()
    val rmsInterceptor = koinInject<RmsProxyInterceptor>()

    LaunchedEffect(Unit) {
        rtmpInterceptor.calls.collect(items::add)
    }

    LaunchedEffect(Unit) {
        configInterceptor.calls.collect(items::add)
    }

    LaunchedEffect(Unit) {
        xmppInterceptor.calls.collect(items::add)
    }

    LaunchedEffect(Unit) {
        rmsInterceptor.calls.collect(items::add)
    }

    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Waiting for League Of Legends client to open..."
            )
        }
        return
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Search") },
                    modifier = Modifier
                        .fillMaxWidth(0.9F)
                )

                MultiSelectDropdown(dropDownItems, selectedItems)
            }

            Switch(
                checked = isDarkColors.value,
                onCheckedChange = { isDarkColors.value = it },
                modifier = Modifier.padding(16.dp)
            )
        }
        LazyColumn {
            itemsIndexed(items.filterBySelection(selectedItems.value).filterByText(searchText)) { index, item ->
                when (item) {
                    is ConfigResponse -> ListItem(headlineContent = { RenderConfigCall(item, index) })
                    is RtmpCall -> ListItem(headlineContent = { RenderRtmpCall(item, index) })
                    is XmppCall -> ListItem(headlineContent = { RenderXmppCall(item, index) })
                    is RmsCall -> ListItem(headlineContent = { RenderRmsCall(item, index) })
                }
            }
        }
    }
}

@Composable
fun RenderRtmpCall(item: RtmpCall, index: Int) {
    var expanded by remember { mutableStateOf(false) }
    var showReadable by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$index - RTMP ${rtmpCallPreview(item)}",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp // Adjust the font size as needed
                    ),
                )
                Button(onClick = { expanded = !expanded }, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                    Text(if (!expanded) "Expand" else "Hide")
                }
            }

            if (expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Button(
                        onClick = { showReadable = !showReadable },
                        modifier = Modifier
                            .padding(8.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Text(if (showReadable) "Show structured" else "Show readable")
                    }
                }
                Column {
                    item.data.forEach {
                        if (showReadable) RenderSelectableText(Amf0PrettyBuilder().write(it).build())
                        else RenderSelectableText(it.prettyPrint())
                    }
                }
            } else {
                // Show summary when not expanded
                Text("${item.data.toString().substring(0, minOf(item.data.toString().length, 50))}...")
            }
        }
    }
}

@Composable
fun RenderConfigCall(item: ConfigCall, index: Int) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$index - CONFIG RESPONSE",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp // Adjust the font size as needed
                    ),
                )
                Button(onClick = { expanded = !expanded }, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                    Text(if (!expanded) "Expand" else "Hide")
                }
            }

            SelectionContainer {
                Text(text = item.url, modifier = Modifier.padding(8.dp))
            }

            if (expanded) {
                Column {
                    RenderSelectableText(item.headers.toMap().prettyPrint())
                    RenderSelectableText(item.data.serializedPretty())
                }
            }
        }
    }
}

@Composable
fun RenderXmppCall(item: XmppCall, index: Int) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$index - XMPP ${xmppCallPreview(item)}",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp // Adjust the font size as needed
                    ),
                )
                Button(onClick = { expanded = !expanded }, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                    Text(if (!expanded) "Expand" else "Hide")
                }
            }

            if (expanded) {
                Column {
                    RenderSelectableText(item.data)
                }
            } else {
                Text("${item.data.substring(0, minOf(item.data.length, 50))}...")
            }
        }
    }
}

@Composable
fun RenderRmsCall(item: RmsCall, index: Int) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$index - RMS ${rmsCallPreview(item)}",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp // Adjust the font size as needed
                    ),
                )
                Button(onClick = { expanded = !expanded }, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                    Text(if (!expanded) "Expand" else "Hide")
                }
            }

            if (expanded) {
                Column {
                    RenderSelectableText(item.data.serializedPretty())
                }
            } else {
                Text("${item.data.serialized().substring(0, minOf(item.data.serialized().length, 50))}...")
            }
        }
    }
}

@Composable
private fun RenderSelectableText(text: String) {
    SelectionContainer {
        TextArea(text)
    }
}

fun rtmpCallPreview(item: RtmpCall) = when (item) {
    is RtmpRequest -> "REQUEST"
    is RtmpResponse -> "RESPONSE"
}

fun xmppCallPreview(item: XmppCall) = when (item) {
    is XmppRequest -> "REQUEST"
    is XmppResponse -> "RESPONSE"
}

fun rmsCallPreview(item: RmsCall) = when (item) {
    is RmsRequest -> "REQUEST"
    is RmsResponse -> "RESPONSE"
}


fun List<Call>.filterBySelection(list: List<String>) = filter {
    when (it) {
        is ConfigResponse -> list.contains("CONFIG")
        is RtmpCall -> list.contains("RTMP")
        is XmppCall -> list.contains("XMPP")
        is RmsCall -> list.contains("RMS")
    }
}

fun List<Call>.filterByText(text: String) = filter {
    if (text.trim().isBlank()) return@filter true
    when (it) {
        is ConfigResponse -> it.data.serialized().contains(text, true)
        is RtmpCall -> it.data.prettyPrint().contains(text, true)
        is XmppCall -> it.data.contains(text, true)
        is RmsCall -> it.data.serialized().contains(text, true)
    }
}