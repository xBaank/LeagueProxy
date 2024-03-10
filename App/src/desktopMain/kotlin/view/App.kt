package view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ApplicationScope
import kotlinx.coroutines.runBlocking
import org.koin.compose.koinInject
import shared.Call
import shared.proxies.interceptors.HttpProxyInterceptor
import shared.proxies.interceptors.RmsProxyInterceptor
import shared.proxies.interceptors.RtmpProxyInterceptor
import shared.proxies.interceptors.XmppProxyInterceptor
import shared.proxies.utils.killRiotClient
import view.other.AlertDialog
import view.theme.DarkColors
import view.theme.LightColors

@Composable
fun ApplicationScope.App(isRiotClientClosed: MutableState<Boolean>) {
    val isDarkColor = remember { mutableStateOf(true) }
    val isSettings = remember { mutableStateOf(false) }
    val items: SnapshotStateList<Call> = remember { mutableStateListOf() }
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
                Settings(isDarkColor, isSettings)
            } else {
                ProxyCalls(isSettings, items)
            }
        }
    }
}