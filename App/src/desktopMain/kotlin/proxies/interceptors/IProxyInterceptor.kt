package proxies.interceptors

interface IProxyInterceptor<T> {
    suspend fun onRequest(value: T): T
    suspend fun onResponse(value: T): T
}