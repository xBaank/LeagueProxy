package view

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import proxies.interceptors.RTMPProxyInterceptor
import proxies.interceptors.RtmpCall
import rtmp.amf0.*


@Composable
fun RtmpCalls() {
    val items: SnapshotStateList<RtmpCall> = remember { mutableStateListOf() }
    val myService = koinInject<RTMPProxyInterceptor>()

    LaunchedEffect(Unit) {
        myService.calls.collect(items::add)
    }

    LazyColumn {
        items(items) { item ->
            ListItem(headlineContent = { RenderRtmpCall(item) })
        }
    }
}

@Preview
@Composable
fun RenderRtmpCall(item: RtmpCall) {
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
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable { expanded = !expanded } // Toggle expanded state on click
        ) {
            Text(
                text = rtmpCallPreview(item),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (expanded) {
                SelectionContainer {
                    Column {
                        item.data.forEach { RenderAmf0Node(it) }
                    }
                }
            } else {
                // Show summary when not expanded
                Text("${item.data.toString().substring(0..50)}...")
            }
        }
    }
}

val padding = 8.dp

//TODO change this to simple text but with a bit of pretty formatting, that will simplify its viewing when trying too see JSON or XML

@Composable
private fun RenderAmf0Node(amf0Node: Amf0Node) {
    Row {
        when (amf0Node) {
            is Amf0String -> Text(
                text = "\"${amf0Node.value}\"",
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis
            )

            is Amf0Number -> Text(
                text = amf0Node.value.toString(),
                style = MaterialTheme.typography.bodyMedium
            )

            is Amf0Boolean -> Text(
                text = amf0Node.value.toString(),
                style = MaterialTheme.typography.bodyMedium
            )

            is Amf0Object -> {
                Column(modifier = Modifier.padding(start = padding)) {
                    for ((key, value) in amf0Node.value) {
                        Row {
                            Text(text = "$key:", style = MaterialTheme.typography.bodyMedium)
                            RenderAmf0Node(value)
                        }
                    }
                }
            }

            is Amf0TypedObject -> {
                Column(modifier = Modifier.padding(start = padding)) {
                    Row {
                        Text(text = "${amf0Node.name}:", style = MaterialTheme.typography.bodyMedium)
                        Column(modifier = Modifier.padding(start = padding)) {
                            for ((key, value) in amf0Node.value) {
                                Row {
                                    Text(text = "$key:", style = MaterialTheme.typography.bodyMedium)
                                    RenderAmf0Node(value)
                                }
                            }
                        }
                    }
                }
            }

            is Amf0ECMAArray -> {
                Column(modifier = Modifier.padding(start = padding)) {
                    for ((key, value) in amf0Node.value) {
                        Column(modifier = Modifier.padding(start = padding)) {
                            Row {
                                Text(text = "$key:", style = MaterialTheme.typography.bodyMedium)
                                RenderAmf0Node(value)
                            }
                        }
                    }
                }
            }

            is Amf0StrictArray -> {
                Column(modifier = Modifier.padding(start = padding)) {
                    Row {
                        for (value in amf0Node.value) {
                            RenderAmf0Node(value)
                        }
                    }
                }
            }

            is Amf0Null -> {
                Text(text = "null", style = MaterialTheme.typography.bodyMedium)
            }

            is Amf0Undefined -> {
                Text(text = "undefined", style = MaterialTheme.typography.bodyMedium)
            }

            else -> {
                Text(text = amf0Node.toString(), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

fun rtmpCallPreview(item: RtmpCall) = when (item) {
    is RtmpCall.RtmpRequest -> "Request"
    is RtmpCall.RtmpResponse -> "Response"
}