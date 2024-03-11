package view

import SettingsManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ApplicationScope
import extensions.getResourceAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.koin.compose.koinInject
import scripting.eval
import shared.Call
import shared.proxies.interceptors.HttpProxyInterceptor
import shared.proxies.interceptors.RmsProxyInterceptor
import shared.proxies.interceptors.RtmpProxyInterceptor
import shared.proxies.interceptors.XmppProxyInterceptor
import shared.proxies.utils.killRiotClient
import shared.proxies.utils.showError
import view.other.AlertDialog
import view.theme.DarkColors
import view.theme.LightColors

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun ApplicationScope.App(isRiotClientClosed: MutableState<Boolean>) {
    val items: SnapshotStateList<Call> = remember { mutableStateListOf() }
    val rtmpInterceptor = koinInject<RtmpProxyInterceptor>()
    val xmppInterceptor = koinInject<XmppProxyInterceptor>()
    val rmsInterceptor = koinInject<RmsProxyInterceptor>()
    val httpProxyInterceptor = koinInject<HttpProxyInterceptor>()
    val settingsManager = koinInject<SettingsManager>()
    val defaultFunction = remember { getResourceAsText("call.kts")?.let(::eval) }
    val scriptFunction: MutableState<((Call) -> Call)?> = remember { mutableStateOf(null) }

    var settings by remember { mutableStateOf(settingsManager.settings.value) }
    val isSettings = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settingsManager.settings.collect {
            settings = it
            launch(Dispatchers.IO) {
                runCatching {
                    if (scriptFunction.value == null)
                        scriptFunction.value = it.scriptFile?.toPath()?.toFile()?.let(::eval)
                    else
                        scriptFunction.value = null
                }.onFailure {
                    showError(it.message ?: "", "Error evaluating the script")
                }
            }
        }
    }


    LaunchedEffect(Unit) {
        flowOf(
            rtmpInterceptor.calls,
            xmppInterceptor.calls,
            rmsInterceptor.calls,
            httpProxyInterceptor.calls
        ).flattenMerge()
            .map {
                runCatching { defaultFunction?.invoke(it) }.onFailure {
                    showError(it.message ?: "", "Error executing the script")
                }.getOrNull() ?: it
            }
            .map {
                runCatching { scriptFunction.value?.invoke(it) }.onFailure {
                    showError(it.message ?: "", "Error executing the script")
                }.getOrNull() ?: it
            }
            .collect { if (settings.isCollecting) items.add(it) }
    }


    MaterialTheme(colorScheme = if (settings.isDarkMode) DarkColors else LightColors) {
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
                Settings(isSettings)
            } else {
                ProxyCalls(isSettings, items)
            }
        }
    }
}