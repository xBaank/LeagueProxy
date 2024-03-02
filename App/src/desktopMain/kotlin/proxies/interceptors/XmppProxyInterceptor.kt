package proxies.interceptors

import kotlinx.coroutines.flow.MutableSharedFlow
import org.w3c.dom.Document
import proxies.interceptors.Call.XmppCall
import proxies.interceptors.Call.XmppCall.XmppRequest
import proxies.interceptors.Call.XmppCall.XmppResponse

class XmppProxyInterceptor : IProxyInterceptor<Document, XmppCall> {
    val calls: MutableSharedFlow<XmppCall> = MutableSharedFlow()

    override suspend fun onRequest(value: Document): XmppCall {
        val call = XmppRequest(value)
        calls.emit(call)
        return call
    }

    override suspend fun onResponse(value: Document): XmppCall {
        val call = XmppResponse(value)
        calls.emit(call)
        return call
    }
}