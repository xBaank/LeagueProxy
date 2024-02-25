package proxies.interceptors

import kotlinx.coroutines.flow.MutableSharedFlow
import proxies.interceptors.RtmpCall.RtmpRequest
import proxies.interceptors.RtmpCall.RtmpResponse
import rtmp.amf0.Amf0Node

class RTMPProxyInterceptor : IProxyInterceptor<List<Amf0Node>, RtmpCall> {
    val calls: MutableSharedFlow<RtmpCall> = MutableSharedFlow()

    override suspend fun onRequest(value: List<Amf0Node>): RtmpCall {
        val request = RtmpRequest(value)
        calls.emit(request)
        return request
    }

    override suspend fun onResponse(value: List<Amf0Node>): RtmpCall {
        val response = RtmpResponse(value)
        calls.emit(response)
        return response
    }
}