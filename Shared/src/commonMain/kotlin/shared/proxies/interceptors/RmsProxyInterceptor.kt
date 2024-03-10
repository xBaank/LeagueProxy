package shared.proxies.interceptors

import kotlinx.coroutines.flow.MutableSharedFlow
import shared.Call
import shared.Call.RmsCall.RmsRequest
import shared.Call.RmsCall.RmsResponse
import simpleJson.JsonNode

class RmsProxyInterceptor : ProxyInterceptor<JsonNode, Call.RmsCall> {
    val calls: MutableSharedFlow<Call.RmsCall> = MutableSharedFlow()

    override suspend fun onRequest(value: JsonNode): Call.RmsCall {
        val request = RmsRequest(value)
        calls.emit(request)
        return request
    }

    override suspend fun onResponse(value: JsonNode): Call.RmsCall {
        val response = RmsResponse(value)
        calls.emit(response)
        return response
    }


}