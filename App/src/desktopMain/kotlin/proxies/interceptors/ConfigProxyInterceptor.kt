package proxies.interceptors

import kotlinx.coroutines.flow.MutableSharedFlow
import proxies.interceptors.Call.ConfigCall
import proxies.interceptors.Call.ConfigCall.ConfigResponse
import simpleJson.JsonNode

class ConfigProxyInterceptor : IProxyInterceptor<JsonNode, ConfigCall> {
    val calls: MutableSharedFlow<ConfigCall> = MutableSharedFlow()

    override suspend fun onRequest(value: JsonNode): ConfigCall {
        TODO("Not yet implemented")
    }

    override suspend fun onResponse(value: JsonNode): ConfigCall {
        val configResponse = ConfigResponse(value)
        calls.emit(configResponse)
        return configResponse
    }
}