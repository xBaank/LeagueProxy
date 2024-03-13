package shared.proxies

import arrow.core.raise.catch
import io.github.oshai.kotlinlogging.KotlinLogging
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
import kotlinx.coroutines.*
import shared.Call.RmsCall.RmsRequest
import shared.Call.RmsCall.RmsResponse
import shared.proxies.interceptors.RmsProxyInterceptor
import shared.proxies.utils.findFreePort
import shared.proxies.utils.gzipArray
import shared.proxies.utils.ungzip
import simpleJson.deserialized
import simpleJson.serialized
import java.time.Duration
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets


private val logger = KotlinLogging.logger {}

class RmsProxy(override val url: String, private val proxyEventHandler: RmsProxyInterceptor) :
    Proxy {
    override val port: Int = findFreePort()
    override val started: CompletableJob = Job()
    private var server: ApplicationEngine? = null

    override suspend fun start() {
        val server = embeddedServer(Netty, port = port) {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(60)
            }
            val client = HttpClient {
                install(ClientWebSockets)
            }
            routing {
                webSocketRaw("{...}") webSocket@{
                    logger.info { "Accepted connection in ${call.request.uri}" }

                    val clientSocket = client.webSocketRawSession(
                        method = HttpMethod.Get
                    ) {
                        val url = this@RmsProxy.url + call.request.uri
                        url(url)
                    }


                    val job1 = launch(Dispatchers.IO) {
                        try {
                            for (frame in incoming) {
                                val node = when (frame) {
                                    is Frame.Text -> frame.readBytes().decodeToString().deserialized()
                                        .getOrNull()

                                    is Frame.Binary ->
                                        catch({ frame.readBytes().ungzip().deserialized().getOrNull() }, { null })

                                    else -> null
                                }

                                if (node != null) {
                                    val response = proxyEventHandler.intercept(RmsRequest(node))
                                    if (frame is Frame.Text) clientSocket.send(Frame.Text(response.data.serialized()))
                                    if (frame is Frame.Binary) clientSocket.send(
                                        Frame.Binary(frame.fin, response.data.serialized().toByteArray().gzipArray())
                                    )
                                    continue
                                }
                                clientSocket.send(frame)
                                clientSocket.flush()
                            }
                        } catch (e: Throwable) {
                            logger.error(e) {}
                            clientSocket.close(e)
                        }
                    }

                    // Launch a coroutine to read messages from the target WebSocket and send them to the client WebSocket
                    val job2 = launch(Dispatchers.IO) {
                        try {
                            for (frame in clientSocket.incoming) {
                                val node = when (frame) {
                                    is Frame.Text -> frame.readBytes().decodeToString().deserialized()
                                        .getOrNull()

                                    is Frame.Binary ->
                                        catch({ frame.readBytes().ungzip().deserialized().getOrNull() }, { null })

                                    else -> null
                                }

                                if (node != null) {
                                    val response = proxyEventHandler.intercept(RmsResponse(node))
                                    if (frame is Frame.Text) send(Frame.Text(response.data.serialized()))
                                    if (frame is Frame.Binary) send(
                                        Frame.Binary(frame.fin, response.data.serialized().toByteArray().gzipArray())
                                    )
                                    continue
                                }

                                send(frame)
                                flush()
                            }
                        } catch (e: Throwable) {
                            logger.error(e) {}
                            close(e)
                        }
                    }

                    joinAll(job1, job2)
                }
            }
        }.start(wait = false)

        this.server = server
        started.complete()
    }

    override fun close() {
        server?.stop(50, 50)
    }
}