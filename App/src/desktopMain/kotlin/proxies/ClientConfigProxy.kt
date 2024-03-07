package proxies

import arrow.core.getOrElse
import extensions.inject
import extensions.port
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import proxies.interceptors.Call
import proxies.interceptors.Call.ConfigCall.ConfigRequest
import proxies.interceptors.Call.ConfigCall.ConfigResponse
import proxies.interceptors.HttpProxyInterceptor
import proxies.utils.findFreePort
import simpleJson.*

//TODO use systemyaml url
private const val configUrl = "https://clientconfig.rpg.riotgames.com"

class ClientConfigProxy(
    private val configProxyInterceptor: HttpProxyInterceptor,
    private val xmppProxies: Map<String, XmppProxy>,
    private val rmsProxies: Set<RmsProxy>,
    private val redEdgeProxies: Set<HttpProxy>,
    private val riotAuthProxy: HttpProxy,
    private val riotAuthenticateProxy: HttpProxy,
    private val rioEntitlementAuthProxy: HttpProxy,
    private val riotAffinityServer: HttpProxy,
) : AutoCloseable {

    val client by inject<HttpClient>()
    val port: Int = findFreePort()
    private var server: NettyApplicationEngine? = null

    fun start() {
        val server = embeddedServer(Netty, port = port) {
            routing {
                route("{...}") {
                    handle {
                        try {
                            val url = "$configUrl${call.request.uri}"
                            val body = call.receiveText()

                            val interceptedRequest = configProxyInterceptor.onRequest(
                                ConfigRequest(
                                    body.deserialized().getOrNull(),
                                    url,
                                    call.request.headers,
                                    call.request.httpMethod,
                                    null
                                )
                            )

                            val response = client.request(url) {
                                method = interceptedRequest.method
                                this.headers.appendAll(interceptedRequest.headers)
                                if (interceptedRequest.data != null) setBody(interceptedRequest.data!!.serialized())
                            }

                            val responseBytes = response.bodyAsText()
                            val json = responseBytes.deserialized().getOrElse { throw it }

                            val interceptedResponse = onConfigResponse(
                                ConfigResponse(
                                    json,
                                    url,
                                    response.headers,
                                    call.request.httpMethod,
                                    response.status
                                )
                            )

                            call.respondText(
                                interceptedResponse.data?.serialized() ?: "",
                                response.contentType(),
                                response.status
                            )
                        } catch (ex: Throwable) {
                            println(ex)
                            throw ex
                        }
                    }
                }
            }
        }.start(wait = false)

        this.server = server
    }

    private suspend fun onConfigResponse(
        value: Call.HttpCall,
    ): Call.HttpCall {
        val json = value.data ?: return configProxyInterceptor.onResponse(value)

        if (json["rms.affinities"].isRight()) {
            json["rms.affinities"].asObject().getOrNull()?.forEach { key, _ ->
                val value = json["rms.affinities"][key].asString().getOrNull() ?: return@forEach
                val proxy =
                    rmsProxies.firstOrNull { it.url.removeSuffix(":443") == value } ?: return@forEach
                json["rms.affinities"][key] = "ws://127.0.0.1:${proxy.port}"
            }
        }

        if (json["keystone.rso-authenticator.service_url"].isRight()) {
            json["keystone.rso-authenticator.service_url"] = "http://127.0.0.1:${riotAuthenticateProxy.port}"
        }

        if (json["keystone.rso_auth.url"].isRight()) {
            json["keystone.rso_auth.url"] = "http://127.0.0.1:${riotAuthProxy.port}"
        }

        if (json["keystone.player-affinity.playerAffinityServiceURL"].isRight()) {
            json["keystone.player-affinity.playerAffinityServiceURL"] = "http://127.0.0.1:${riotAffinityServer.port}"
        }

        if (json["keystone.entitlements.url"].isRight()) {
            json["keystone.entitlements.url"] = "http://127.0.0.1:${rioEntitlementAuthProxy.port}"
        }

        if (json["lol.client_settings.league_edge.url"].isRight()) {
            val value = json["lol.client_settings.league_edge.url"].asString().getOrNull()
            val proxy = redEdgeProxies.firstOrNull { it.url == value }
            if (proxy != null)
                json["lol.client_settings.league_edge.url"] = "http://127.0.0.1:${proxy.port}"
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

        value.data = json
        return configProxyInterceptor.onResponse(value)
    }

    override fun close() {
        server?.stop(50, 50)
    }
}