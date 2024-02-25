package proxies.interceptors

import rtmp.amf0.Amf0Node

class RTMPProxyInterceptor : IProxyInterceptor<List<Amf0Node>> {
    override suspend fun onRequest(value: List<Amf0Node>): List<Amf0Node> {
        TODO("Not yet implemented")
    }

    override suspend fun onResponse(value: List<Amf0Node>): List<Amf0Node> {
        TODO("Not yet implemented")
    }
}