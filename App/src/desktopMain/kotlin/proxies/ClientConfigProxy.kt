package proxies

import arrow.core.getOrElse
import extensions.port
import io.ktor.client.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import proxies.interceptors.Call.ConfigCall.ConfigResponse
import proxies.interceptors.ConfigProxyInterceptor
import proxies.utils.findFreePort
import simpleJson.*
import java.net.URI

//TODO use systemyaml url
private const val configUrl = "https://clientconfig.rpg.riotgames.com"

class ClientConfigProxy(
    private val configProxyInterceptor: ConfigProxyInterceptor,
    private val xmppProxies: Map<String, XmppProxy>,
    private val rmsProxies: Set<RmsProxy>,
) : AutoCloseable {

    val client = HttpClient() {
        install(ContentEncoding) {
            gzip()
        }
    }

    val port: Int = findFreePort()
    private var server: NettyApplicationEngine? = null

    fun start() {
        val server = embeddedServer(Netty, port = port) {
            routing {
                route("{...}") {
                    handle {
                        val url = "$configUrl${call.request.uri}"

                        val response = client.request(url) {
                            method = call.request.httpMethod
                            call.request.headers.forEach { s, strings ->
                                if (s.equals("host", true)) {
                                    this.headers.append("Host", URI.create(configUrl).host)
                                    return@forEach
                                }
                                val value = strings.firstOrNull() ?: return@forEach
                                this.headers.append(s, value)
                            }
                        }

                        val responseBytes = response.bodyAsText()
                        val json = responseBytes.deserialized().getOrElse { throw it }

                        if (json["rms.affinities"].isRight()) {
                            json["rms.affinities"].asObject().getOrNull()?.forEach { key, _ ->
                                val value = json["rms.affinities"][key].asString().getOrNull() ?: return@forEach
                                val proxy =
                                    rmsProxies.firstOrNull { it.url.removeSuffix(":443") == value } ?: return@forEach
                                json["rms.affinities"][key] = "ws://127.0.0.1:${proxy.port}"
                            }
                        }


                        if (json["chat.host"].isRight()) {
                            val chatHost = json["chat.host"].asString().getOrElse { throw it }
                            val xmppHost = xmppProxies.values.first { it.host == chatHost }
                            json["chat.host"] = "127.0.0.1"

                            if (json["chat.port"].isRight()) {
                                json["chat.port"] = xmppHost.serverSocket.localAddress.port
                            }


                            if (json["chat.use_tls.enabled"].isRight()) {
                                json["chat.use_tls.enabled"] = false
                            }

                            json["chat.affinities"].asObject().getOrNull()?.forEach { key, _ ->
                                json["chat.affinities"][key] = "127.0.0.1"
                            }
                        }

                        configProxyInterceptor.onResponse(ConfigResponse(json, url, response.headers))

                        call.respondText(
                            json.serialized(),
                            ContentType.Application.Json,
                            response.status
                        )
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