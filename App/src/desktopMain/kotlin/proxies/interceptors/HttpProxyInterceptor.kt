package proxies.interceptors

import extensions.getOrThrow
import extensions.port
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.flow.MutableSharedFlow
import proxies.interceptors.Call.ConfigCall.ConfigResponse
import proxies.interceptors.Call.HttpCall
import proxies.interceptors.Call.RiotAuthCall.RiotAuthResponse
import simpleJson.*

class HttpProxyInterceptor : IProxyInterceptor<HttpCall, HttpCall> {
    val calls: MutableSharedFlow<HttpCall> = MutableSharedFlow()

    private fun fixRiotAuth(value: RiotAuthResponse) {
        if (value.body is Body.Json && value.url == "https://auth.riotgames.com/.well-known/openid-configuration") {
            val endpoint = "http://127.0.0.1:${value.port}"
            val asString = value.body.data.serialized().replace("https://auth.riotgames.com", endpoint)
            value.body.data = asString.deserialized().getOrThrow()
        }
    }

    private fun fixHeaders(value: HttpCall) {
        value.headers = value.headers.toMap().mapNotNull {
            if (it.key.equals("Host", true)) null
            else if (it.key.equals("Content-Length", true)) null
            else if (it.key.equals("Transfer-Encoding", true)) null
            else it.key to it.value
        }.let {
            headers {
                it.forEach { (first, second) ->
                    second.forEach {
                        append(first, it)
                    }
                }
            }
        }
    }

    private fun patchConfigConfiguration(response: ConfigResponse) {
        val body = response.body
        if (body is Body.Text || body is Body.Raw) {
            return
        }

        val json = (response.body as Body.Json).data

        if (json["rms.affinities"].isRight()) {
            json["rms.affinities"].asObject().getOrNull()?.forEach { key, _ ->
                val value = json["rms.affinities"][key].asString().getOrNull() ?: return@forEach
                val proxy = response.rmsProxies.firstOrNull { it.url.removeSuffix(":443") == value } ?: return@forEach
                json["rms.affinities"][key] = "ws://127.0.0.1:${proxy.port}"
            }
        }

        if (json["keystone.rso-authenticator.service_url"].isRight()) {
            json["keystone.rso-authenticator.service_url"] = "http://127.0.0.1:${response.riotAuthenticateProxy.port}"
        }

        if (json["keystone.rso_auth.url"].isRight()) {
            json["keystone.rso_auth.url"] = "http://127.0.0.1:${response.riotAuthProxy.port}"
        }

        if (json["keystone.player-affinity.playerAffinityServiceURL"].isRight()) {
            json["keystone.player-affinity.playerAffinityServiceURL"] =
                "http://127.0.0.1:${response.riotAffinityServer.port}"
        }

        if (json["keystone.entitlements.url"].isRight()) {
            json["keystone.entitlements.url"] = "http://127.0.0.1:${response.rioEntitlementAuthProxy.port}"
        }

        if (json["lol.client_settings.league_edge.url"].isRight()) {
            val value = json["lol.client_settings.league_edge.url"].asString().getOrNull()
            val proxy = response.redEdgeProxies.firstOrNull { it.url == value }
            if (proxy != null)
                json["lol.client_settings.league_edge.url"] = "http://127.0.0.1:${proxy.port}"
        }


        if (json["chat.host"].isRight()) {
            val chatHost = json["chat.host"].asString().getOrThrow()
            val xmppHost = response.xmppProxies.values.first { it.host == chatHost }
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
    }

    override suspend fun onRequest(value: HttpCall): HttpCall {
        fixHeaders(value)
        calls.emit(value)
        return value
    }

    override suspend fun onResponse(value: HttpCall): HttpCall {
        if (value is RiotAuthResponse) fixRiotAuth(value)
        if (value is ConfigResponse) patchConfigConfiguration(value)
        fixHeaders(value)
        calls.emit(value)
        return value
    }
}