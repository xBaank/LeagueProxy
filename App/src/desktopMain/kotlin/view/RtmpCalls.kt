package view

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import proxies.interceptors.RTMPProxyInterceptor
import proxies.interceptors.RtmpCall
import proxies.utils.Amf0PrettyBuilder
import rtmp.amf0.Amf0Node


@Composable
fun RtmpCalls() {
    val items: SnapshotStateList<RtmpCall> = remember { mutableStateListOf() }
    val myService = koinInject<RTMPProxyInterceptor>()

    LaunchedEffect(Unit) {
        myService.calls.collect(items::add)
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

//TODO change this to simple text but with a bit of pretty formatting, that will simplify its viewing when trying too see JSON or XML

@Composable
private fun RenderAmf0Node(amf0Node: Amf0Node) {
    TextArea(Amf0PrettyBuilder().write(amf0Node).build())
}

fun rtmpCallPreview(item: RtmpCall) = when (item) {
    is RtmpCall.RtmpRequest -> "Request"
    is RtmpCall.RtmpResponse -> "Response"
}