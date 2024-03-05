package proxies

import extensions.inject
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import proxies.interceptors.Call.HttpCall
import proxies.interceptors.HttpProxyInterceptor
import proxies.utils.findFreePort
import simpleJson.JsonNode
import simpleJson.deserialized
import simpleJson.serialized


class HttpProxy(
    val url: String,
    val proxyInterceptor: HttpProxyInterceptor,
    val requestCreator: (
        data: JsonNode?,
        url: String,
        headers: Headers,
        method: HttpMethod,
        status: HttpStatusCode?,
    ) -> HttpCall,
    val responseCreator: (
        port: Int,
        data: JsonNode?,
        url: String,
        headers: Headers,
        method: HttpMethod,
        status: HttpStatusCode?,
    ) -> HttpCall,
) : AutoCloseable {

    val client by inject<HttpClient>()
    val port: Int = findFreePort()
    private var server: NettyApplicationEngine? = null

    fun start() {
        val server = embeddedServer(Netty, port = port, configure = { tcpKeepAlive = true }) {
            install(CORS) {
                anyHost()
                allowHeaders { true }
                HttpMethod.DefaultMethods.forEach { allowMethod(it) }
                allowHeader("access-control-expose-headers")
            }
            routing {
                route("{...}") {
                    handle {
                        try {
                            val url = "$url${call.request.uri}"
                            val body = if (call.request.contentType() == ContentType.Application.Json)
                                call.receiveText().deserialized().getOrNull()
                            else null

                            val interceptedRequest = proxyInterceptor.onRequest(
                                requestCreator(
                                    body,
                                    url,
                                    call.request.headers,
                                    call.request.httpMethod,
                                    null
                                )
                            )

                            //Hide original Host
                            val response = client.request(url) {
                                method = interceptedRequest.method
                                this.headers.appendAll(interceptedRequest.headers)
                                if (interceptedRequest.data != null) setBody(interceptedRequest.data!!.serialized())
                            }


                            val responseBytes = response.bodyAsText()
                            val json = responseBytes.deserialized().getOrNull()

                            val interceptedResponse = proxyInterceptor.onResponse(
                                responseCreator(
                                    port,
                                    json,
                                    url,
                                    response.headers,
                                    call.request.httpMethod,
                                    response.status
                                )
                            )

                            interceptedResponse.headers.forEach { s, strings ->
                                strings.forEach {
                                    call.response.headers.append(s, it)
                                }
                            }



                            if (interceptedResponse.data == null) {
                                call.respondBytes(
                                    status = interceptedResponse.statusCode ?: HttpStatusCode.OK
                                ) {
                                    byteArrayOf()
                                }
                            } else {
                                val data = interceptedResponse.data!!.serialized()
                                call.response.headers.append(
                                    "Content-Length",
                                    data.toByteArray().count().toString()
                                )

                                call.respondText(
                                    text = interceptedResponse.data!!.serialized(),
                                    status = interceptedResponse.statusCode ?: HttpStatusCode.OK
                                )
                            }
                        } catch (ex: Throwable) {
                            println(ex)
                            throw ex
                        }
                    }
                }
            }
        }.start(wait = false)

        this.server = server
    }

    override fun close() {
        server?.stop(500, 500)
    }
}