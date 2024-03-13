package shared.proxies.interceptors


import kotlinx.coroutines.channels.Channel
import shared.Call.HttpCall

class HttpProxyInterceptor {
    val calls: Channel<HttpCall> = Channel()
    val interceptedCalls: Channel<HttpCall> = Channel()

    suspend fun intercept(call: HttpCall): HttpCall {
        calls.send(call)
        return interceptedCalls.receive()
    }
}