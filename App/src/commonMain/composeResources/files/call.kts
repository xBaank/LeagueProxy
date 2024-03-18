package files

import io.ktor.http.*
import io.ktor.util.*
import shared.Body
import shared.Call
import shared.extensions.getOrThrow
import shared.extensions.port
import simpleJson.*

{ value: Call ->
    fun fixRiotAuth(value: Call.RiotAuthCall.RiotAuthResponse) {
        if (value.body is Body.Json && value.url == "https://auth.riotgames.com/.well-known/openid-configuration") {
            val endpoint = "http://127.0.0.1:${value.port}"
            val asString = (value.body as Body.Json).data.serialized().replace("https://auth.riotgames.com", endpoint)
            (value.body as Body.Json).data = asString.deserialized().getOrThrow()
        }
    }

    fun fixHeaders(value: Call.HttpCall) {
        value.headers = value.headers.toMap().mapNotNull {
            if (it.key.equals("Host", true)) null
            else if (it.key.equals("Content-Length", true)) null
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

    fun patchConfigConfiguration(response: Call.ConfigCall.ConfigResponse) {
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
            response.riotAuthenticateProxy.url = json["keystone.rso-authenticator.service_url"].asString().getOrThrow()
            json["keystone.rso-authenticator.service_url"] = "http://127.0.0.1:${response.riotAuthenticateProxy.port}"
        }

        if (json["keystone.rso_auth.url"].isRight()) {
            response.riotAuthProxy.url = json["keystone.rso_auth.url"].asString().getOrThrow()
            json["keystone.rso_auth.url"] = "http://127.0.0.1:${response.riotAuthProxy.port}"
            json["keystone.rso_auth.use_new_login_api"] = false
        }

        if (json["lol.client_settings.player_platform_edge.url"].isRight()) {
            response.riotPlatformEdge.url = json["lol.client_settings.player_platform_edge.url"].asString().getOrThrow()
            json["lol.client_settings.player_platform_edge.url"] = "http://127.0.0.1:${response.riotPlatformEdge.port}"
            json["lol.client_settings.player_platform_edge.enable"] = true
            json["lol.client_settings.match_history.player_platform_edge.enabled"] = true
        }

        if (json["keystone.player-affinity.playerAffinityServiceURL"].isRight()) {
            response.riotAffinityServer.url =
                json["keystone.player-affinity.playerAffinityServiceURL"].asString().getOrThrow()
            json["keystone.player-affinity.playerAffinityServiceURL"] =
                "http://127.0.0.1:${response.riotAffinityServer.port}"
        }

        if (json["keystone.entitlements.url"].isRight()) {
            response.rioEntitlementAuthProxy.url = json["keystone.entitlements.url"].asString().getOrThrow()
            json["keystone.entitlements.url"] = "http://127.0.0.1:${response.rioEntitlementAuthProxy.port}"
        }

        if (json["keystone.player-reporting.report_collector_url"].isRight()) {
            response.riotReportCollector.url = "https://euc1-red.pp.sgp.pvp.net" //TDO remove this hardcoded

            json["keystone.player-reporting.report_collector_url"] =
                "http://127.0.0.1:${response.riotReportCollector.port}"
            json["keystone.player-reporting.report_collector_url_by_affinities"]["eu"] =
                "http://127.0.0.1:${response.riotReportCollector.port}"

            json["keystone.client.feature_flags.playerReporting.enabled"] = true
        }

        if (json["lol.client_settings.league_edge.url"].isRight()) {
            val value = json["lol.client_settings.league_edge.url"].asString().getOrNull()
            val proxy = response.redEdgeProxies.firstOrNull { it.url == value }
            if (proxy != null)
                json["lol.client_settings.league_edge.url"] = "http://127.0.0.1:${proxy.port}"
        }

        if (json["lol.client_settings.store.use_ledge"].isRight()) {
            json["lol.client_settings.store.use_ledge"] = true
        }


        if (json["chat.host"].isRight()) {
            val chatHost = json["chat.host"].asString().getOrThrow()
            val xmppHost = response.xmppProxies.values.first { it.url == chatHost }
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

    if (value is Call.RiotAuthCall.RiotAuthResponse) fixRiotAuth(value)
    if (value is Call.ConfigCall.ConfigResponse) patchConfigConfiguration(value)
    if (value is Call.HttpCall) fixHeaders(value)

    value
}
