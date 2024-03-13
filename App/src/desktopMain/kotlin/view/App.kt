package view

import SettingsManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ApplicationScope
import exceptions.ScriptException
import extensions.lastModified
import io.github.irgaly.kfswatch.KfsDirectoryWatcher
import io.github.irgaly.kfswatch.KfsEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.xbaank.leagueproxy.app.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.DIRECTORY_SEPARATOR
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.ExperimentalResourceApi
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

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalResourceApi::class)
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

    val defaultFunction: ((Call) -> Call) = remember {
        runBlocking { Res.readBytes("files/call.kts").decodeToString().let(::eval) }
    }

    val scriptFunction: MutableState<((Call) -> Call)?> = remember { mutableStateOf(null) }

    val onScriptFailure = { ex: Throwable ->
        if (ex is ScriptException) showError(ex.message ?: "", "Error evaluating the script")
        else logger.error(ex) {}
    }


    LaunchedEffect(Unit) {
        val watcher: KfsDirectoryWatcher = KfsDirectoryWatcher(this, Dispatchers.IO, rawEventEnabled = true)
        var lastmodified: Long = 0

        launch(Dispatchers.IO) {
            watcher.onEventFlow.collect {
                when (it.event) {
                    KfsEvent.Modify -> {
                        val path = settings.scriptFile?.toPath()
                        if ((it.targetDirectory + DIRECTORY_SEPARATOR + it.path).toPath() == path) {
                            val currentLastModified = path.lastModified()
                            if (lastmodified < currentLastModified) {
                                scriptFunction.value = path.toFile().let(::eval)
                                lastmodified = currentLastModified
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }

        settingsManager.settings.collect {
            settings = it
            launch(Dispatchers.IO) {
                runCatching {
                    if (scriptFunction.value == null && it.scriptFile != null) {
                        val parent = it.scriptFile.toPath().parent.toString()
                        scriptFunction.value = it.scriptFile.toPath().toFile().let(::eval)
                        watcher.removeAll()
                        watcher.add(parent)
                    } else {
                        scriptFunction.value = null
                        watcher.removeAll()
                    }
                }.onFailure(onScriptFailure)
                    .onFailure { settingsManager.settings.value = settings.copy(scriptFile = null) }
            }
        }
    }

    LaunchedEffect(Unit) {
        flowOf(
            rtmpInterceptor.calls.consumeAsFlow(),
            xmppInterceptor.calls.consumeAsFlow(),
            rmsInterceptor.calls.consumeAsFlow(),
            httpProxyInterceptor.calls.consumeAsFlow()
        ).flattenMerge()
            .map {
                runCatching { defaultFunction.invoke(it) }
                    .onFailure(onScriptFailure)
                    .getOrNull() ?: it
            }
            .map {
                runCatching { scriptFunction.value?.invoke(it) }
                    .onFailure(onScriptFailure)
                    .onFailure { settingsManager.settings.value = settings.copy(scriptFile = null) }
                    .getOrNull() ?: it
            }
            .onEach {
                when (it) {
                    is Call.HttpCall -> httpProxyInterceptor.interceptedCalls.send(it)
                    is Call.RmsCall -> rmsInterceptor.interceptedCalls.send(it)
                    is Call.RtmpCall -> rtmpInterceptor.interceptedCalls.send(it)
                    is Call.XmppCall -> xmppInterceptor.interceptedCalls.send(it)
                }
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