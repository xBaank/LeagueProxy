package shared.proxies.interceptors

import kotlinx.coroutines.flow.MutableSharedFlow
import shared.Call.XmppCall
import shared.Call.XmppCall.XmppRequest
import shared.Call.XmppCall.XmppResponse

class XmppProxyInterceptor : ProxyInterceptor<String, XmppCall> {
    val calls: MutableSharedFlow<XmppCall> = MutableSharedFlow()

    override suspend fun onRequest(value: String): XmppCall {
        val call = XmppRequest(value)
        calls.emit(call)
        return call
    }

    override suspend fun onResponse(value: String): XmppCall {
        val call = XmppResponse(value)
        calls.emit(call)
        return call
    }
}