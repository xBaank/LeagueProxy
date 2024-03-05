package proxies.interceptors

import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.flow.MutableSharedFlow
import proxies.interceptors.Call.HttpCall
import proxies.interceptors.Call.RedEdgeCall.RedEdgeResponse
import proxies.interceptors.Call.RiotAuthCall.RiotAuthResponse
import simpleJson.*

class HttpProxyInterceptor : IProxyInterceptor<HttpCall, HttpCall> {
    val calls: MutableSharedFlow<HttpCall> = MutableSharedFlow()

    private fun fixRiotAuth(value: RiotAuthResponse) {
        if (value.data != null && value.url == "https://auth.riotgames.com/.well-known/openid-configuration") {
            val endpoint = "http://127.0.0.1:${value.port}"
            val asString = value.data!!.serialized().replace("https://auth.riotgames.com", endpoint)
            value.data = asString.deserialized().getOrNull()
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


    override suspend fun onRequest(value: HttpCall): HttpCall {
        fixHeaders(value)
        calls.emit(value)
        return value
    }

    private fun fixCraft(value: HttpCall) {
        if (value.data != null && value.url == "https://euw-red.lol.sgp.pvp.net/personalized-offers/v1/player/offers?lang=es_ES") {
            val json = value.data!!
            if (json["offers"].isRight()) {
                json["offers"].asArray().getOrNull()?.forEach {
                    val originalPrice = it["originalPrice"].asInt().getOrNull() ?: 0
                    it["originalPrice"] = originalPrice
                    it["discountPrice"] = 0
                    it["discountAmount"] = 100
                }
            }
        }
    }

    override suspend fun onResponse(value: HttpCall): HttpCall {
        if (value is RiotAuthResponse) fixRiotAuth(value)
        if (value is RedEdgeResponse) fixCraft(value)
        fixHeaders(value)
        calls.emit(value)
        return value
    }
}