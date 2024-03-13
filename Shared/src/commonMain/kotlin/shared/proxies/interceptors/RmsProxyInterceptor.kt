package shared.proxies.interceptors

import kotlinx.coroutines.channels.Channel
import shared.Call.RmsCall

class RmsProxyInterceptor {
    val calls: Channel<RmsCall> = Channel()
    val interceptedCalls: Channel<RmsCall> = Channel()

    suspend fun intercept(call: RmsCall): RmsCall {
        calls.send(call)
        return interceptedCalls.receive()
    }
}