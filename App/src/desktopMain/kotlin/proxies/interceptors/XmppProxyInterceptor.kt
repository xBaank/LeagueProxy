package proxies.interceptors

import kotlinx.coroutines.flow.MutableSharedFlow
import proxies.interceptors.Call.XmppCall
import proxies.interceptors.Call.XmppCall.XmppRequest
import proxies.interceptors.Call.XmppCall.XmppResponse

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