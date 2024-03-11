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
import io.github.irgaly.kfswatch.KfsDirectoryWatcher
import io.github.irgaly.kfswatch.KfsEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.DIRECTORY_SEPARATOR
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

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun ApplicationScope.App(isRiotClientClosed: MutableState<Boolean>) {
    val items: SnapshotStateList<Call> = remember { mutableStateListOf() }
    val rtmpInterceptor = koinInject<RtmpProxyInterceptor>()
    val xmppInterceptor = koinInject<XmppProxyInterceptor>()
    val rmsInterceptor = koinInject<RmsProxyInterceptor>()
    val httpProxyInterceptor = koinInject<HttpProxyInterceptor>()
    val settingsManager = koinInject<SettingsManager>()

    var settings by remember { mutableStateOf(settingsManager.settings.value) }
    val isSettings = remember { mutableStateOf(false) }

    val defaultFunction = remember { getResourceAsText("call.kts")?.let(::eval) }
    val scriptFunction: MutableState<((Call) -> Call)?> = remember { mutableStateOf(null) }


    LaunchedEffect(Unit) {
        val watcher: KfsDirectoryWatcher = KfsDirectoryWatcher(this, Dispatchers.IO)

        launch(Dispatchers.IO) {
            watcher.onEventFlow.collect {
                when (it.event) {
                    KfsEvent.Create -> Unit
                    KfsEvent.Delete -> Unit
                    KfsEvent.Modify -> {
                        if ((it.targetDirectory + DIRECTORY_SEPARATOR + it.path).toPath() == settings.scriptFile?.toPath()) {
                            logger.info { "Reloading script" }
                            scriptFunction.value = settings.scriptFile?.toPath()?.toFile()?.let(::eval)
                            logger.info { "Reloaded script" }
                        }
                    }
                }
            }
        }

        settingsManager.settings.collect {
            settings = it
            launch(Dispatchers.IO) {
                runCatching {
                    if (scriptFunction.value == null && it.scriptFile != null) {
                        val parent = it.scriptFile.toPath().parent.toString()
                        logger.info { "loading script" }
                        scriptFunction.value = it.scriptFile.toPath().toFile().let(::eval)
                        watcher.removeAll()
                        watcher.add(parent)
                        logger.info { "loaded script" }
                    } else {
                        scriptFunction.value = null
                        watcher.removeAll()
                    }
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