package proxies

import arrow.core.getOrElse
import extensions.port
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import okhttp3.OkHttpClient
import okhttp3.Request
import proxies.interceptors.Call.ConfigCall.ConfigResponse
import proxies.interceptors.ConfigProxyInterceptor
import proxies.utils.findFreePort
import ru.gildor.coroutines.okhttp.await
import simpleJson.*
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

//TODO use systemyaml url
private const val configUrl = "https://clientconfig.rpg.riotgames.com"

class ClientConfigProxy(
    private val configProxyInterceptor: ConfigProxyInterceptor,
    private val xmppProxies: Map<String, XmppProxy>,
    private val rmsProxies: Set<RmsProxy>,
) : AutoCloseable {

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

    val port: Int = findFreePort()
    private var server: NettyApplicationEngine? = null

    fun start() {
        val server = embeddedServer(Netty, port = port) {
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

                    if (json["rms.affinities"].isRight()) {
                        json["rms.affinities"].asObject().getOrNull()?.forEach { key, _ ->
                            val value = json["rms.affinities"][key].asString().getOrNull() ?: return@forEach
                            val proxy =
                                rmsProxies.firstOrNull { it.url.removeSuffix(":443") == value } ?: return@forEach
                            json["rms.affinities"][key] = "ws://127.0.0.1:${proxy.port}"
                        }
                    }

                    if (json["chat.host"].isRight()) {
                        val chatHost = json["chat.host"].asString().getOrElse { throw it }
                        val xmppHost = xmppProxies.values.first { it.host == chatHost }
                        json["chat.host"] = "127.0.0.1"

                        if (json["chat.port"].isRight()) {
                            json["chat.port"] = xmppHost.serverSocket.localAddress.port
                        }


                        if (json["chat.use_tls.enabled"].isRight()) {
                            json["chat.use_tls.enabled"] = false
                        }

                        json["chat.affinities"].asObject().getOrNull()?.forEach { key, _ ->
                            json["chat.affinities"][key] = "127.0.0.1"
                        }
                    }

                    configProxyInterceptor.onResponse(ConfigResponse(json, url, response.headers))

                    call.respondText(
                        json.serialized(),
                        ContentType.Application.Json,
                        HttpStatusCode.fromValue(response.code)
                    )
                }
            }
        }.start(wait = false)

        this.server = server
    }

    override fun close() {
        server?.stop(500, 500)
    }
}