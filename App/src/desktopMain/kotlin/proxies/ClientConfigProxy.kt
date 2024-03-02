package proxies

import arrow.core.getOrElse
import extensions.inject
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import okhttp3.OkHttpClient
import okhttp3.Request
import proxies.interceptors.ConfigProxyInterceptor
import ru.gildor.coroutines.okhttp.await
import simpleJson.deserialized
import simpleJson.serialized
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

private const val configUrl = "https://clientconfig.rpg.riotgames.com"

class ClientConfigProxy {
    val configProxyInterceptor by inject<ConfigProxyInterceptor>()

    private val trustAllCerts = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }

    private fun trustAllSsl(): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf(trustAllCerts), SecureRandom())
        return sslContext.socketFactory
    }

    private val httpClient = OkHttpClient.Builder()
        .sslSocketFactory(trustAllSsl(), trustAllCerts)
        .hostnameVerifier { _, _ -> true }
        .build()

    var port: Int? = null

    fun start() {
        val server = embeddedServer(Netty) {
            routing {
                get("{...}") {
                    val url = "$configUrl${call.request.uri}"
                    val userAgent = call.request.userAgent()
                    val auth = call.request.headers["Authorization"]
                    val jwt = call.request.headers["X-Riot-Entitlements-JWT"]

                    val response = httpClient.newCall(
                        Request.Builder().url(url)
                            .apply { if (userAgent != null) header("User-Agent", userAgent) }
                            .apply { if (auth != null) header("Authorization", auth) }
                            .apply { if (jwt != null) header("X-Riot-Entitlements-JWT", jwt) }
                            .build()
                    ).await()

                    val responseBytes = response.use { it.body!!.string() }
                    val json = responseBytes.deserialized().getOrElse { throw it }

                    configProxyInterceptor.onResponse(json)

                    call.respondText(
                        json.serialized(),
                        ContentType.Application.Json,
                        HttpStatusCode.fromValue(response.code)
                    )
                }
            }
        }.start(wait = false)
        port = server.environment.connectors.first().port
    }
}