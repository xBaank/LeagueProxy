import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import io.ktor.utils.io.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import proxies.LeagueNotFoundException
import proxies.utils.*
import kotlin.system.exitProcess

suspend fun main() = awaitApplication {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        launch { proxies { isVisible = true } }
    }
    Window(onCloseRequest = ::exitApplication, visible = isVisible, title = "ComposeDemo") {
        App()
    }
}

private suspend fun proxies(onStarted: () -> Unit) = coroutineScope {
    if (isRiotClientRunning()) {
        val wantsToClose = askForClose(
            "Do you want to close it? If you dont close it LeagueProxy won't be launched",
            "Riot Client is already running"
        )
        if (wantsToClose) killRiotClient()
        else return@coroutineScope
    }

    val hosts = getHosts()
    proxies(hosts).forEach { launch { it.start() } }

    runCatching {
        val job = launch { startClient(hosts) { exitProcess(0) } }
        onStarted()
        job.join()
    }.onFailure {
        if (it is LeagueNotFoundException) {
            showError(it.message ?: "", "League of Legends not found")
            return@onFailure
        }
        showError(it.stackTraceToString(), it.message ?: "An error happened")
        it.printStack()
    }

}

