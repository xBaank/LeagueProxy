package proxies

import extensions.inject
import extensions.isGzip
import extensions.isJson
import extensions.isText
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import proxies.interceptors.Body
import proxies.interceptors.Call.HttpCall
import proxies.interceptors.HttpProxyInterceptor
import proxies.utils.findFreePort
import proxies.utils.gzipArray
import simpleJson.deserialized
import simpleJson.serialized


class HttpProxy(
    override val url: String,
    val proxyInterceptor: HttpProxyInterceptor,
    val requestCreator: (
        data: Body,
        url: String,
        headers: Headers,
        method: HttpMethod,
        status: HttpStatusCode?,
    ) -> HttpCall,
    val responseCreator: (
        data: Body,
        url: String,
        headers: Headers,
        method: HttpMethod,
        status: HttpStatusCode?,
    ) -> HttpCall,
    override val port: Int = findFreePort(),
) : Proxy {

    val client by inject<HttpClient>()
    private var server: NettyApplicationEngine? = null
    override val started: CompletableJob = Job()

    override suspend fun start() {
        val server = embeddedServer(Netty, port = port) {
            routing {
                route("{...}") {
                    handle {
                        val url = "$url${call.request.uri}"

                        try {
                            val body = when {
                                call.request.headers.isJson() -> {
                                    val text = call.receiveText().replace("\\u0001", "")
                                    val json = text.deserialized().getOrNull()
                                    if (json != null) Body.Json(json) else Body.Text(text)
                                }

                                call.request.headers.isText() -> {
                                    val text = call.receiveText()
                                    Body.Text(text)
                                }

                                else -> {
                                    Body.Raw(call.receiveChannel().toByteArray())
                                }
                            }

                            val interceptedRequest = proxyInterceptor.onRequest(
                                requestCreator(
                                    body,
                                    url,
                                    call.request.headers,
                                    call.request.httpMethod,
                                    null
                                )
                            )

                            val response = client.request(url) {
                                method = interceptedRequest.method
                                this.headers.appendAll(interceptedRequest.headers)
                                when (val body = interceptedRequest.body) {
                                    is Body.Json -> setBody(body.data.serialized())
                                    is Body.Raw -> setBody(body.data)
                                    is Body.Text -> setBody(body.data)
                                }
                            }

                            val responseBody = when {
                                response.headers.isJson() -> {
                                    val text = response.bodyAsText().replace("\\u0001", "")
                                    val json = text.deserialized().getOrNull()
                                    if (json != null) Body.Json(json) else Body.Text(text)
                                }

                                response.headers.isText() -> {
                                    val text = response.bodyAsText()
                                    Body.Text(text)
                                }

                                else -> {
                                    Body.Raw(response.bodyAsChannel().toByteArray())
                                }
                            }

                            val interceptedResponse = proxyInterceptor.onResponse(
                                responseCreator(
                                    responseBody,
                                    url,
                                    response.headers,
                                    call.request.httpMethod,
                                    response.status
                                )
                            )

                            interceptedResponse.headers.forEach { s, strings ->
                                strings.forEach {
                                    call.response.headers.append(s, it, safeOnly = false)
                                }
                            }

                            val byteArrayBody = when (val body = interceptedResponse.body) {
                                is Body.Json -> body.data.serialized().toByteArray()
                                is Body.Raw -> body.data
                                is Body.Text -> body.data.toByteArray()
                            }

                            if (interceptedResponse.headers.isGzip()) {
                                call.respondBytes(
                                    status = interceptedResponse.statusCode ?: HttpStatusCode.OK
                                ) { byteArrayBody.gzipArray() }
                            } else {
                                call.respondBytes(
                                    status = interceptedResponse.statusCode ?: HttpStatusCode.OK
                                ) { byteArrayBody }
                            }

                        } catch (ex: Throwable) {
                            println(ex)
                            println(url)
                            throw ex
                        }
                    }
                }
            }
        }.start(wait = false)

        this.server = server
        started.complete()
    }

    override fun close() {
        server?.stop(500, 500)
    }
}