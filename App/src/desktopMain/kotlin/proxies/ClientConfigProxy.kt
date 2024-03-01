package proxies

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await
import simpleJson.deserialized
import simpleJson.serialized

private const val configUrl = "https://clientconfig.rpg.riotgames.com"

class ClientConfigProxy {
    private val client = OkHttpClient.Builder().build()
    var port: Int? = null

    fun start() {
        val server = embeddedServer(Netty) {
            routing {
                get("{...}") {
                    val url = "$configUrl${call.request.uri}"
                    val headers = call.request.headers.entries()
                    val contentType = call.request.contentType()

                    val response = client.newCall(
                        Request.Builder().url(url)
                            .apply {
                                headers.forEach {
                                    val value = it.value.firstOrNull() ?: return@forEach
                                    header(it.key, value)
                                }
                            }
                            .build()
                    ).await()

                    val responseString = response.use { it.body!!.string() }
                    val json = responseString.deserialized().getOrNull()

                    call.respondText(
                        json?.serialized() ?: "",
                        contentType,
                        HttpStatusCode.fromValue(response.code)
                    )
                }
            }
        }.start(wait = false)

        port = server.environment.connectors.first().port
    }
}