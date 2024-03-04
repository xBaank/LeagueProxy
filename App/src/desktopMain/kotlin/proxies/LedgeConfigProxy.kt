package proxies

import arrow.core.getOrElse
import io.ktor.client.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import proxies.utils.findFreePort
import simpleJson.deserialized
import simpleJson.serialized
import java.net.URI

class LedgeConfigProxy(
    val url: String,
) : AutoCloseable {

    val client = HttpClient() {
        install(ContentEncoding) {
            gzip()
        }
    }

    val port: Int = findFreePort()
    private var server: NettyApplicationEngine? = null

    fun start() {
        val server = embeddedServer(Netty, port = port) {
            routing {
                route("{...}") {
                    handle {
                        val url = "$url${call.request.uri}"
                        val body = call.receiveText()

                        val response = client.request(url) {
                            method = call.request.httpMethod
                            call.request.headers.forEach { s, strings ->
                                if (s.equals("host", true)) {
                                    this.headers.append("Host", URI.create(url).host)
                                    return@forEach
                                }
                                val value = strings.firstOrNull() ?: return@forEach
                                this.headers.append(s, value)
                            }
                            setBody(body)
                        }

                        val responseBytes = response.bodyAsText()
                        val json = responseBytes.deserialized().getOrElse { throw it }

                        call.respondText(
                            json.serialized(),
                            ContentType.Application.Json,
                            response.status
                        )
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