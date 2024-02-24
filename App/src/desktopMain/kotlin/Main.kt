import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import io.ktor.utils.io.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import proxies.LeagueNotFoundException
import proxies.utils.*

suspend fun main() = awaitApplication {
    var isVisible by remember { mutableStateOf(false) }
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

    val hosts = getHosts()
    proxies(hosts) { type, nodes ->
        println(type)
        println(nodes)
        nodes
    }.forEach { launch { it.start() } }

    runCatching {
        val job = launch { startClient(hosts = hosts, onClose = onClose) }
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

