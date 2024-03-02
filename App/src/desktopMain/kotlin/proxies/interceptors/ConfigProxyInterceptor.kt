package proxies.interceptors

import kotlinx.coroutines.flow.MutableSharedFlow
import proxies.interceptors.Call.ConfigCall

class ConfigProxyInterceptor : IProxyInterceptor<ConfigCall, ConfigCall> {
    val calls: MutableSharedFlow<ConfigCall> = MutableSharedFlow()

    override suspend fun onRequest(value: ConfigCall): ConfigCall {
        TODO("Not yet implemented")
    }

    override suspend fun onResponse(value: ConfigCall): ConfigCall {
        calls.emit(value)
        return value
    }
}