package proxies

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import proxies.interceptors.Call.XmppCall
import proxies.interceptors.IProxyInterceptor
import kotlin.coroutines.cancellation.CancellationException


fun XmppProxy(host: String, port: Int, proxyEventHandler: IProxyInterceptor<String, XmppCall>): XmppProxy {
    val selectorManager = SelectorManager(Dispatchers.IO)
    val socketServer = aSocket(selectorManager).tcp().bind()

    return XmppProxy(socketServer, host, port, proxyEventHandler)
}

class XmppProxy internal constructor(
    val serverSocket: ServerSocket,
    val host: String,
    val port: Int,
    private val proxyEventHandler: IProxyInterceptor<String, XmppCall>,
) {

    suspend fun start() = coroutineScope {
        while (isActive) {
            val socket = serverSocket.accept()
            println("Accepted xmpp connection from ${socket.remoteAddress} in ${socket.localAddress}")
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
        val clientSocket = aSocket(selectorManager).tcp().connect(host, port).tls(Dispatchers.IO)

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
                if (string.isNotBlank()) proxyEventHandler.onResponse(string)
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
                if (string.isNotBlank()) proxyEventHandler.onRequest(string)

                clientWriteChannel.writeFully(byteArray, 0, read)
            }
        }
    }
}