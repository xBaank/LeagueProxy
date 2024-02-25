package view

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.style.TextAlign
import org.koin.compose.koinInject
import proxies.interceptors.RTMPProxyInterceptor
import rtmp.amf0.Amf0Node

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RtmpCalls() {
    val requestItems: SnapshotStateList<List<Amf0Node>> = remember { mutableStateListOf(listOf()) }
    val responseItems: SnapshotStateList<List<Amf0Node>> = remember { mutableStateListOf(listOf()) }
    val myService = koinInject<RTMPProxyInterceptor>()

    LaunchedEffect(Unit) {
        myService.requests.collect(requestItems::add)
    }

    LaunchedEffect(Unit) {
        myService.responses.collect(responseItems::add)
    }

    LazyColumn {
        items(requestItems.map { it.toString() }) { item ->
            ListItem { Text(color = MaterialTheme.colorScheme.primary, text = item, textAlign = TextAlign.Center) }
        }
    }
}