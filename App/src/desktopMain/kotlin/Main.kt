import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import io.ktor.utils.io.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import proxies.LeagueNotFoundException
import proxies.client.ClientProxy
import proxies.client.SystemYamlPatcher
import proxies.utils.askForClose
import proxies.utils.isRiotClientRunning
import proxies.utils.killRiotClient
import proxies.utils.showError

suspend fun main() = awaitApplication {
    var isVisible by remember { mutableStateOf(false) }

    startKoin {
        modules(module)
    }

    LaunchedEffect(Unit) {
        launch { proxies(onStarted = { isVisible = true }, onClose = ::exitApplication) }
    }

    Window(onCloseRequest = ::exitApplication, visible = isVisible, title = "ComposeDemo") {
        App()
    }
}

private suspend fun proxies(onStarted: () -> Unit, onClose: () -> Unit) = coroutineScope {
    if (isRiotClientRunning()) {
        val wantsToClose = askForClose(
            "Do you want to close it? If you dont close it LeagueProxy won't be launched",
            "Riot Client is already running"
        )
        if (wantsToClose) killRiotClient()
        else {
            onClose()
            return@coroutineScope
        }
    }

    val clientProxy = ClientProxy(SystemYamlPatcher(), onClientClose = onClose)
    clientProxy.use {
        launch { clientProxy.startProxies() }
        runCatching {
            val job = launch { clientProxy.startClient() }
            onStarted()
            job.join()
        }.onFailure {
            if (it is LeagueNotFoundException) {
                showError(it.message ?: "", "League of Legends not found")
                return@onFailure
            }
            if (it is CancellationException) {
                return@onFailure
            }
            showError(it.stackTraceToString(), it.message ?: "An error happened")
            it.printStack()
        }
    }
}

