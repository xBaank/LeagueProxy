package shared.proxies.interceptors

import kotlinx.coroutines.channels.Channel
import shared.Call.RtmpCall

class RtmpProxyInterceptor {
    val calls: Channel<RtmpCall> = Channel()
    val interceptedCalls: Channel<RtmpCall> = Channel()

    suspend fun intercept(call: RtmpCall): RtmpCall {
        calls.send(call)
        return interceptedCalls.receive()
    }
}