package proxies

import kotlinx.coroutines.Job

interface Proxy : AutoCloseable {
    val url: String
    val port: Int
    val started: Job
    suspend fun start()
}