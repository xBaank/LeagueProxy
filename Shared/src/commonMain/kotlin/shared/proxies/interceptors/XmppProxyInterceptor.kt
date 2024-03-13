package shared.proxies.interceptors

import kotlinx.coroutines.channels.Channel
import shared.Call.XmppCall

class XmppProxyInterceptor {
    val calls: Channel<XmppCall> = Channel()
    val interceptedCalls: Channel<XmppCall> = Channel()

    suspend fun intercept(call: XmppCall): XmppCall {
        calls.send(call)
        return interceptedCalls.receive()
    }
}