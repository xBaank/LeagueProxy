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
import proxies.interceptors.Call
import proxies.interceptors.Call.ConfigCall
import proxies.interceptors.Call.RtmpCall
import proxies.interceptors.ConfigProxyInterceptor
import proxies.interceptors.RTMPProxyInterceptor
import proxies.utils.Amf0PrettyBuilder
import simpleJson.serialized
import simpleJson.serializedPretty


@Composable
fun RtmpCalls() {
    var searchText by remember { mutableStateOf("") }
    val items: SnapshotStateList<Call> = remember { mutableStateListOf() }
    val rtmpInterceptor = koinInject<RTMPProxyInterceptor>()
    val configInterceptor = koinInject<ConfigProxyInterceptor>()

    LaunchedEffect(Unit) {
        rtmpInterceptor.calls.collect(items::add)
    }

    LaunchedEffect(Unit) {
        configInterceptor.calls.collect(items::add)
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
        TextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        LazyColumn {
            itemsIndexed(items.filterByText(searchText)) { index, item ->
                when (item) {
                    is ConfigCall.ConfigResponse -> ListItem(headlineContent = { RenderConfigCall(item, index) })
                    is RtmpCall -> ListItem(headlineContent = { RenderRtmpCall(item, index) })
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
                Text("${item.data.toString().substring(0..50)}...")
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
private fun RenderSelectableText(text: String) {
    SelectionContainer {
        TextArea(text)
    }
}

fun rtmpCallPreview(item: RtmpCall) = when (item) {
    is RtmpCall.RtmpRequest -> "REQUEST"
    is RtmpCall.RtmpResponse -> "RESPONSE"
}

fun SnapshotStateList<Call>.filterByText(text: String) = filter {
    if (text.trim().isBlank()) return@filter true
    when (it) {
        is ConfigCall.ConfigResponse -> it.data.serialized().contains(text, true)
        is RtmpCall.RtmpRequest -> it.data.prettyPrint().contains(text, true)
        is RtmpCall.RtmpResponse -> it.data.prettyPrint().contains(text, true)
    }
}