package shared.proxies

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import shared.Call.XmppCall.XmppRequest
import shared.Call.XmppCall.XmppResponse
import shared.proxies.interceptors.XmppProxyInterceptor
import kotlin.coroutines.cancellation.CancellationException


private val logger = KotlinLogging.logger {}

fun XmppProxy(host: String, port: Int, proxyEventHandler: XmppProxyInterceptor): XmppProxy {
    val selectorManager = SelectorManager(Dispatchers.IO)
    val socketServer = aSocket(selectorManager).tcp().bind()

    return XmppProxy(socketServer, host, port, proxyEventHandler)
}

class XmppProxy internal constructor(
    val serverSocket: ServerSocket,
    override val url: String,
    override val port: Int,
    private val proxyEventHandler: XmppProxyInterceptor,
) : Proxy {
    override val started: CompletableJob = Job()

    override suspend fun start() = coroutineScope {
        while (isActive) {
            val socketJob = async { serverSocket.accept() }
            started.complete()
            val socket = socketJob.await()
            logger.info { "Accepted rtmp connection from ${socket.remoteAddress} in ${socket.localAddress}" }
            launch(Dispatchers.IO) { handle(socket) }
        }
    }

    private suspend fun handle(socket: Socket) = coroutineScope {
        runCatching {
            handleSocket(socket)
        }.onFailure {
            when (it) {
                is ClosedReceiveChannelException -> return@coroutineScope
                is CancellationException -> return@coroutineScope
                else -> throw it
            }
        }
    }

    private suspend fun handleSocket(socket: Socket) = coroutineScope {
        val selectorManager = SelectorManager(Dispatchers.IO)
        val clientSocket = aSocket(selectorManager).tcp().connect(url, port).tls(Dispatchers.IO)

        val serverReadChannel = socket.openReadChannel()
        val serverWriteChannel = socket.openWriteChannel(autoFlush = true)
        val clientReadChannel = clientSocket.openReadChannel()
        val clientWriteChannel = clientSocket.openWriteChannel(autoFlush = true)

        //TODO Fix this with incremental parser lix SAX?

        launch(Dispatchers.IO) {
            val byteArray = ByteArray((1024 * 1024) * 10)
            while (isActive) {
                val read = clientReadChannel.readAvailable(byteArray)
                if (read == 0) continue
                if (read < 0) {
                    socket.close()
                    clientSocket.close()
                    cancel("Socket closed")
                    return@launch
                }
                val string = byteArray.copyOfRange(0, read).decodeToString()
                if (string.isNotBlank()) proxyEventHandler.intercept(XmppResponse(string))
                serverWriteChannel.writeFully(byteArray, 0, read)
            }
        }


        launch(Dispatchers.IO) {
            val byteArray = ByteArray((1024 * 1024) * 10)
            while (isActive) {
                val read = serverReadChannel.readAvailable(byteArray)
                if (read == 0) continue
                if (read < 0) {
                    socket.close()
                    clientSocket.close()
                    cancel("Socket closed")
                    return@launch
                }
                val string = byteArray.copyOfRange(0, read).decodeToString()
                if (string.isNotBlank()) proxyEventHandler.intercept(XmppRequest(string))

                clientWriteChannel.writeFully(byteArray, 0, read)
            }
        }
    }

    override fun close() {
        serverSocket.close()
    }

}