package view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ApplicationScope
import extensions.getResourceAsText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.koin.compose.koinInject
import scripting.eval
import shared.Call
import shared.proxies.interceptors.HttpProxyInterceptor
import shared.proxies.interceptors.RmsProxyInterceptor
import shared.proxies.interceptors.RtmpProxyInterceptor
import shared.proxies.interceptors.XmppProxyInterceptor
import shared.proxies.utils.killRiotClient
import view.other.AlertDialog
import view.theme.DarkColors
import view.theme.LightColors

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun ApplicationScope.App(isRiotClientClosed: MutableState<Boolean>) {
    val isDarkColor = remember { mutableStateOf(true) }
    val isSettings = remember { mutableStateOf(false) }
    val isCollecting = remember { mutableStateOf(true) }
    val items: SnapshotStateList<Call> = remember { mutableStateListOf() }
    val rtmpInterceptor = koinInject<RtmpProxyInterceptor>()
    val xmppInterceptor = koinInject<XmppProxyInterceptor>()
    val rmsInterceptor = koinInject<RmsProxyInterceptor>()
    val httpProxyInterceptor = koinInject<HttpProxyInterceptor>()
    val defaultFunction = remember { getResourceAsText("call.kts")?.let(::eval) }

    LaunchedEffect(Unit) {
        flowOf(
            rtmpInterceptor.calls,
            xmppInterceptor.calls,
            rmsInterceptor.calls,
            httpProxyInterceptor.calls
        ).flattenMerge().map { defaultFunction?.invoke(it) ?: it }.collect {
            if (isCollecting.value) items.add(it)
        }
    }


    MaterialTheme(colorScheme = if (isDarkColor.value) DarkColors else LightColors) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            if (!isRiotClientClosed.value) {
                AlertDialog(
                    onDismissRequest = ::exitApplication,
                    onConfirmation = {
                        runBlocking { killRiotClient() }
                        isRiotClientClosed.value = true
                    },
                    dialogTitle = "Riot Client is already running",
                    dialogText = "Do you want to close it? If you dont close it LeagueProxy won't be launched"
                )
                return@Surface
            }

            if (isSettings.value) {
                Settings(isDarkColor, isSettings, isCollecting)
            } else {
                ProxyCalls(isSettings, items)
            }
        }
    }
}