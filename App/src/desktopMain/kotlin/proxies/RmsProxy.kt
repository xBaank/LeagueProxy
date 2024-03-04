package proxies

import io.ktor.client.*
import io.ktor.client.plugins.websocket.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import proxies.utils.findFreePort
import java.time.Duration
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets


class RmsProxy(val url: String) : AutoCloseable {
    val port: Int = findFreePort()
    private var server: NettyApplicationEngine? = null

    fun start() {
        val server = embeddedServer(Netty, port = port) {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(60)
            }
            val client = HttpClient {
                install(ClientWebSockets)
            }
            routing {
                webSocketRaw("{...}") webSocket@{
                    println("Accepted connection in ${call.request.uri}")
                    val reqHeaders = call.request.headers

                    val clientSocket = client.webSocketRawSession(
                        method = HttpMethod.Get
                    ) {
                        url(this@RmsProxy.url + call.request.uri)
                        headers.appendAll(reqHeaders)
                    }
                    launch(Dispatchers.IO) {
                        try {
                            for (frame in incoming) {
                                clientSocket.send(frame)
                            }
                        } catch (e: Throwable) {
                            clientSocket.close(e)
                        }
                    }

                    // Launch a coroutine to read messages from the target WebSocket and send them to the client WebSocket
                    launch(Dispatchers.IO) {
                        try {
                            for (frame in clientSocket.incoming) {
                                send(frame)
                            }
                        } catch (e: Throwable) {
                            close(e)
                        }
                    }
                }
            }
        }.start(wait = false)

        this.server = server
    }

    override fun close() {
        server?.stop(500, 500)
    }
}