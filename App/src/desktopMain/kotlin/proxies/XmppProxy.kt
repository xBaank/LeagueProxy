package proxies

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.w3c.dom.Document
import proxies.interceptors.Call.XmppCall
import proxies.interceptors.IProxyInterceptor
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.coroutines.cancellation.CancellationException


fun XmppProxy(host: String, port: Int, proxyEventHandler: IProxyInterceptor<Document, XmppCall>): XmppProxy {
    val selectorManager = SelectorManager(Dispatchers.IO)
    val socketServer = aSocket(selectorManager).tcp().bind()

    return XmppProxy(socketServer, host, port, proxyEventHandler)
}

class XmppProxy internal constructor(
    val serverSocket: ServerSocket,
    val host: String,
    val port: Int,
    private val proxyEventHandler: IProxyInterceptor<Document, XmppCall>,
) {
    private val factory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
    private val builder: DocumentBuilder = factory.newDocumentBuilder()

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

        //TODO Fix this
        
        launch(Dispatchers.IO) {
            val byteArray = ByteArray(1024 * 1024)
            val read = clientReadChannel.readAvailable(byteArray)
            val doc: Document = builder.parse(byteArray.inputStream(0, read))

            proxyEventHandler.onResponse(doc)

            val transformerFactory = TransformerFactory.newInstance()
            val transformer: Transformer = transformerFactory.newTransformer()
            val stringWriter = StringWriter()
            transformer.transform(DOMSource(doc), StreamResult(stringWriter))
            val result = stringWriter.toString().toByteArray()

            serverWriteChannel.writeFully(result)
        }


        launch(Dispatchers.IO) {
            val byteArray = ByteArray(1024 * 1024)
            val read = serverReadChannel.readAvailable(byteArray)
            val doc: Document = builder.parse(byteArray.inputStream(0, read))

            proxyEventHandler.onRequest(doc)

            val transformerFactory = TransformerFactory.newInstance()
            val transformer: Transformer = transformerFactory.newTransformer()
            val stringWriter = StringWriter()
            transformer.transform(DOMSource(doc), StreamResult(stringWriter))
            val result = stringWriter.toString().toByteArray()

            clientWriteChannel.writeFully(result)
        }
    }
}