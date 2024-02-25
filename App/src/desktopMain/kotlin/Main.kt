import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import client.ClientProxy
import client.SystemYamlPatcher
import exceptions.LeagueNotFoundException
import io.ktor.utils.io.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import proxies.utils.isRiotClientRunning
import proxies.utils.showError
import view.App

suspend fun main() {
    startKoin { modules(module) }

    awaitApplication {
        val isRiotClientClosed = remember { mutableStateOf(runBlocking { !isRiotClientRunning() }) }

        LaunchedEffect(isRiotClientClosed.value) {
            if (!isRiotClientClosed.value) return@LaunchedEffect
            launch { proxies(onStarted = {}, onClose = ::exitApplication) }
        }

        Window(onCloseRequest = ::exitApplication, title = "ComposeDemo") {
            App(isRiotClientClosed)
        }
    }
}

private suspend fun proxies(onStarted: () -> Unit, onClose: () -> Unit) = coroutineScope {
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
