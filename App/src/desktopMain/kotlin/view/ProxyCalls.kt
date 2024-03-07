package view

import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import extensions.*
import io.ktor.util.*
import org.koin.compose.koinInject
import proxies.interceptors.*
import proxies.interceptors.Call.*
import proxies.interceptors.Call.ConfigCall.ConfigRequest
import proxies.interceptors.Call.ConfigCall.ConfigResponse
import proxies.interceptors.Call.RedEdgeCall.RedEdgeRequest
import proxies.interceptors.Call.RedEdgeCall.RedEdgeResponse
import proxies.interceptors.Call.RiotAuthCall.RiotAuthRequest
import proxies.interceptors.Call.RiotAuthCall.RiotAuthResponse
import proxies.interceptors.Call.RmsCall.RmsRequest
import proxies.interceptors.Call.RmsCall.RmsResponse
import proxies.interceptors.Call.RtmpCall.RtmpRequest
import proxies.interceptors.Call.RtmpCall.RtmpResponse
import proxies.interceptors.Call.XmppCall.XmppRequest
import proxies.interceptors.Call.XmppCall.XmppResponse
import proxies.utils.Amf0PrettyBuilder


@Composable
fun ProxyCalls(isDarkColors: MutableState<Boolean>) {
    var searchText by remember { mutableStateOf("") }
    val items: SnapshotStateList<Call> = remember { mutableStateListOf() }
    val dropDownItems = mutableListOf("XMPP", "RTMP", "CONFIG", "RMS", "RED EDGE", "RIOT AUTH")
    val selectedItems = remember { mutableStateOf(dropDownItems.toList()) }
    val rtmpInterceptor = koinInject<RtmpProxyInterceptor>()
    val xmppInterceptor = koinInject<XmppProxyInterceptor>()
    val rmsInterceptor = koinInject<RmsProxyInterceptor>()
    val httpProxyInterceptor = koinInject<HttpProxyInterceptor>()

    LaunchedEffect(Unit) {
        rtmpInterceptor.calls.collect(items::add)
    }

    LaunchedEffect(Unit) {
        xmppInterceptor.calls.collect(items::add)
    }

    LaunchedEffect(Unit) {
        rmsInterceptor.calls.collect(items::add)
    }

    LaunchedEffect(Unit) {
        httpProxyInterceptor.calls.collect(items::add)
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
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .padding(16.dp)
            ) {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Search by url or body") },
                    modifier = Modifier
                        .padding(bottom = 10.dp)
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
        val lazyColumnSatate = rememberLazyListState()
        LazyColumn(state = lazyColumnSatate, modifier = Modifier.simpleVerticalScrollbar(state = lazyColumnSatate)) {
            itemsIndexed(
                items.asSequence().filterBySelection(selectedItems.value).filterByText(searchText).toList()
            ) { index, item ->
                when (item) {
                    is HttpCall -> ListItem(headlineContent = { RenderHttpCall(item, index) })
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
                    text = "$index - ${callPreview(item)}",
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
fun RenderHttpCall(item: HttpCall, index: Int) {
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
                    text = "$index - ${callPreview(item)}",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp // Adjust the font size as needed
                    ),
                )
                Button(onClick = { expanded = !expanded }, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                    Text(if (!expanded) "Expand" else "Hide")
                }
            }

            @Composable
            fun headerText(value: String) = Text(
                value,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
            )

            if (item.statusCode != null) {
                headerText("Status")
                RenderSelectableText(item.statusCode?.value.toString())
            }

            headerText("Url")
            RenderSelectableText(item.url)


            headerText("Method")
            RenderSelectableText(item.method.value)

            if (expanded) {
                Column {
                    headerText("Headers")
                    RenderSelectableText(item.headers.toMap().serializedMemo())

                    if (!item.body.isEmpty()) {
                        headerText("Body")
                        RenderSelectableText(item.body.serializedPrettyMemoCutted()) { item.body.serialized() }
                    }
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
                    text = "$index - ${callPreview(item)}",
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
                    text = "$index - ${callPreview(item)}",
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
                    RenderSelectableText(item.data.serializedMemoCutted())
                }
            } else {
                Text(
                    "${
                        item.data.serializedMemoCutted()
                            .substring(0, minOf(item.data.serializedMemoCutted().length, 50))
                    }..."
                )
            }
        }
    }
}

@Composable
private fun RenderSelectableText(text: String, originalTextF: (() -> String)? = null) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    ContextMenuDataProvider(
        items = {
            listOf(
                ContextMenuItem("Copy all") {
                    clipboardManager.setText(AnnotatedString((originalTextF?.invoke() ?: text)))
                },
            )
        }
    ) {
        TextArea(text)
    }
}

fun callPreview(item: Call) = when (item) {
    is RedEdgeRequest -> "RED EDGE REQUEST"
    is RedEdgeResponse -> "RED EDGE RESPONSE"
    is ConfigResponse -> "CONFIG RESPONSE"
    is ConfigRequest -> "CONFIG REQUEST"
    is RmsRequest -> "RMS REQUEST"
    is RmsResponse -> "RMS RESPONSE"
    is RtmpRequest -> "RTMP REQUEST"
    is RtmpResponse -> "RTMP RESPONSE"
    is XmppRequest -> "XMPP REQUEST"
    is XmppResponse -> "XMPP RESPONSE"
    is RiotAuthRequest -> "RIOT AUTH REQUEST"
    is RiotAuthResponse -> "RIOT AUTH RESPONSE"
}


fun Sequence<Call>.filterBySelection(list: List<String>) = filter {
    when (it) {
        is ConfigCall -> list.contains("CONFIG")
        is RtmpCall -> list.contains("RTMP")
        is XmppCall -> list.contains("XMPP")
        is RmsCall -> list.contains("RMS")
        is RedEdgeCall -> list.contains("RED EDGE")
        is RiotAuthCall -> list.contains("RIOT AUTH")
    }
}

fun Sequence<Call>.filterByText(text: String) = filter {
    if (text.trim().isBlank()) return@filter true
    when (it) {
        is HttpCall -> it.body.serializedPrettyMemoCutted().contains(text, true) || it.url.contains(
            text,
            true
        )

        is RtmpCall -> it.data.serializedMemo().contains(text, true)
        is XmppCall -> it.data.contains(text, true)
        is RmsCall -> it.data.serializedPrettyMemoCutted().contains(text, true)
    }
}