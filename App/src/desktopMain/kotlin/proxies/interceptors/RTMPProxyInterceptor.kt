package proxies.interceptors

import kotlinx.coroutines.flow.MutableSharedFlow
import rtmp.amf0.Amf0Node

class RTMPProxyInterceptor : IProxyInterceptor<List<Amf0Node>> {
    val requests: MutableSharedFlow<List<Amf0Node>> = MutableSharedFlow()
    val responses: MutableSharedFlow<List<Amf0Node>> = MutableSharedFlow()
    
    override suspend fun onRequest(value: List<Amf0Node>): List<Amf0Node> {
        requests.emit(value)
        return value
    }

    override suspend fun onResponse(value: List<Amf0Node>): List<Amf0Node> {
        responses.emit(value)
        return value
    }
}