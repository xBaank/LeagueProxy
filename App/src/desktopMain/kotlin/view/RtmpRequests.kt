package view

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import proxies.interceptors.RTMPProxyInterceptor
import proxies.interceptors.RtmpCall


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RtmpCalls() {
    val items: SnapshotStateList<RtmpCall> = remember { mutableStateListOf() }
    val myService = koinInject<RTMPProxyInterceptor>()

    LaunchedEffect(Unit) {
        myService.calls.collect(items::add)
    }

    LazyColumn {
        items(items) { item ->
            ListItem { RenderRtmpCall(item) }
        }
    }
}

@Preview
@Composable
fun RenderRtmpCall(item: RtmpCall) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        backgroundColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clickable { expanded = !expanded } // Toggle expanded state on click
        ) {
            Text(
                text = rtmpCallPreview(item),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (expanded) {
                // Show full content when expanded
                Text(item.data.toString())
            } else {
                // Show summary when not expanded
                Text("${item.data.toString().substring(0..50)}...")
            }
        }
    }
}

fun rtmpCallPreview(item: RtmpCall) = when (item) {
    is RtmpCall.RtmpRequest -> "Request"
    is RtmpCall.RtmpResponse -> "Response"
}