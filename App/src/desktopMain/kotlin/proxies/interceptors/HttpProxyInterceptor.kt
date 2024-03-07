package proxies.interceptors

import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.flow.MutableSharedFlow
import proxies.interceptors.Call.HttpCall
import proxies.interceptors.Call.RiotAuthCall.RiotAuthResponse
import simpleJson.deserialized
import simpleJson.serialized

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

    override suspend fun onResponse(value: HttpCall): HttpCall {
        if (value is RiotAuthResponse) fixRiotAuth(value)
        fixHeaders(value)
        calls.emit(value)
        return value
    }
}