package proxies

import extensions.inject
import extensions.isJson
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
import io.ktor.util.*
import io.ktor.utils.io.jvm.javaio.*
import proxies.interceptors.Call.HttpCall
import proxies.interceptors.HttpProxyInterceptor
import proxies.utils.findFreePort
import proxies.utils.gzip
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
                allowCredentials = true
                allowHeader("access-control-expose-headers")
            }
            routing {
                route("{...}") {
                    handle {
                        val url = "$url${call.request.uri}"

                        try {
                            val body =
                                if (call.request.headers.isJson()) {
                                    val text = call.receiveText().replace("\\u0001", "")
                                    text.deserialized().getOrNull()
                                } else null

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
                                else if (interceptedRequest.headers.isJson() && interceptedRequest.data == null) Unit
                                else setBody(call.receiveChannel().toByteArray())
                            }

                            val json =
                                if (response.headers.isJson()) {
                                    val text = response.bodyAsText()
                                    text.deserialized().getOrNull()
                                } else null

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
                                call.respondOutputStream(
                                    status = interceptedResponse.statusCode ?: HttpStatusCode.OK
                                ) {
                                    response.bodyAsChannel().copyTo(this)
                                }
                            } else if (interceptedResponse.headers["Content-Encoding"].equals("gzip", true)) {
                                call.respondBytes(
                                    status = interceptedResponse.statusCode ?: HttpStatusCode.OK
                                ) {
                                    interceptedResponse.data!!.serialized().gzip()
                                }
                            } else {
                                call.respondText(
                                    text = interceptedResponse.data!!.serialized(),
                                    status = interceptedResponse.statusCode ?: HttpStatusCode.OK
                                )
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
    }

    override fun close() {
        server?.stop(500, 500)
    }
}