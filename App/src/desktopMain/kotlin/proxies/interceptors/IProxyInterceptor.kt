package proxies.interceptors

interface IProxyInterceptor<T, R> {
    suspend fun onRequest(value: T): R
    suspend fun onResponse(value: T): R
}