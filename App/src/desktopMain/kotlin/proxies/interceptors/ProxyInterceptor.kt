package proxies.interceptors

interface ProxyInterceptor<T, R> {
    suspend fun onRequest(value: T): R
    suspend fun onResponse(value: T): R
}