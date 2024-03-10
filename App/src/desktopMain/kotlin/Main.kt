import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import client.CreateClientProxy
import client.SystemYamlPatcher
import exceptions.LeagueNotFoundException
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import shared.extensions.inject
import shared.proxies.utils.isRiotClientRunning
import shared.proxies.utils.showError
import view.App
import kotlin.system.exitProcess

suspend fun main() {
    startKoin { modules(module) }

    awaitApplication {
        val isRiotClientClosed = remember { mutableStateOf(runBlocking { !isRiotClientRunning() }) }

        LaunchedEffect(isRiotClientClosed.value) {
            if (!isRiotClientClosed.value) return@LaunchedEffect
            launch {
                proxies(onStarted = {}, onClose = ::exitApplication)
            }
        }

        Window(onCloseRequest = ::exitApplication, title = "TraitorsBlade") {
            App(isRiotClientClosed)
        }
    }

    exitProcess(0)
}

private suspend fun proxies(onStarted: () -> Unit, onClose: () -> Unit) = coroutineScope {
    val patcher by inject<SystemYamlPatcher>()
    val clientProxy = CreateClientProxy(patcher, onClientClose = onClose)
    clientProxy.use {
        launch(Dispatchers.IO) { clientProxy.startProxies() }
        runCatching {
            launch(Dispatchers.IO) { clientProxy.startClient() }.also { onStarted() }.join()
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
