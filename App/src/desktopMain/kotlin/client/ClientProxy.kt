package client

import com.github.pgreze.process.process
import extensions.inject
import extensions.port
import io.ktor.http.*
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import proxies.*
import proxies.interceptors.*
import proxies.interceptors.Call.ConfigCall.ConfigRequest
import proxies.interceptors.Call.ConfigCall.ConfigResponse
import proxies.interceptors.Call.RedEdgeCall.RedEdgeRequest
import proxies.interceptors.Call.RedEdgeCall.RedEdgeResponse
import proxies.interceptors.Call.RiotAuthCall.RiotAuthRequest
import proxies.interceptors.Call.RiotAuthCall.RiotAuthResponse
import proxies.utils.findFreePort

fun CreateClientProxy(systemYamlPatcher: SystemYamlPatcher, onClientClose: () -> Unit): ClientProxy {
    val rtmpProxyInterceptor by inject<RtmpProxyInterceptor>()
    val xmppProxyInterceptor by inject<XmppProxyInterceptor>()
    val rmsProxyInterceptor by inject<RmsProxyInterceptor>()
    val httpProxyInterceptor by inject<HttpProxyInterceptor>()

    val rtmpProxies = systemYamlPatcher.rtmpHostsByRegion.map { (region, lcds) ->
        val proxyClient = RtmpProxy(lcds.host, lcds.port, rtmpProxyInterceptor)
        region to proxyClient
    }.toMap()

    val rmsProxies = systemYamlPatcher.rmsHostsByRegion.map { host ->
        val proxyClient = RmsProxy(host.host, rmsProxyInterceptor)
        proxyClient
    }.toSet()

    val redEdgeProxies = systemYamlPatcher.redEdgeHostsByRegion.map { host ->
        val proxyClient = HttpProxy(host.host, httpProxyInterceptor, ::RedEdgeRequest, ::RedEdgeResponse)
        proxyClient
    }.toSet()

    val riotPlatformEdgeProxy = run {
        val host = "https://euc1-red.pp.sgp.pvp.net"
        val port = findFreePort()
        val proxyClient = HttpProxy(
            url = host,
            port = port,
            proxyInterceptor = httpProxyInterceptor,
            requestCreator = ::RiotAuthRequest,
            responseCreator = { data: Body, url: String, headers: Headers, method: HttpMethod, status: HttpStatusCode? ->
                RiotAuthResponse(port, data, url, headers, method, status)
            })
        proxyClient
    }

    val rioAuthenticateProxy = run {
        val host = "https://authenticate.riotgames.com"
        val port = findFreePort()
        val proxyClient = HttpProxy(
            url = host,
            port = port,
            proxyInterceptor = httpProxyInterceptor,
            requestCreator = ::RiotAuthRequest,
            responseCreator = { data: Body, url: String, headers: Headers, method: HttpMethod, status: HttpStatusCode? ->
                RiotAuthResponse(port, data, url, headers, method, status)
            })
        proxyClient
    }

    val rioAuthProxy = run {
        val host = "https://auth.riotgames.com"
        val port = findFreePort()
        val proxyClient = HttpProxy(
            url = host,
            port = port,
            proxyInterceptor = httpProxyInterceptor,
            requestCreator = ::RiotAuthRequest,
            responseCreator = { data: Body, url: String, headers: Headers, method: HttpMethod, status: HttpStatusCode? ->
                RiotAuthResponse(port, data, url, headers, method, status)
            }
        )
        proxyClient
    }

    val rioAffinityProxy = run {
        val host = "https://riot-geo.pas.si.riotgames.com"
        val port = findFreePort()
        val proxyClient = HttpProxy(
            url = host,
            port = port,
            proxyInterceptor = httpProxyInterceptor,
            requestCreator = ::RiotAuthRequest,
            responseCreator = { data: Body, url: String, headers: Headers, method: HttpMethod, status: HttpStatusCode? ->
                RiotAuthResponse(port, data, url, headers, method, status)
            }
        )
        proxyClient
    }

    val rioEntitlementAuthProxy = run {
        val host = "https://entitlements.auth.riotgames.com/api/token/v1"
        val port = findFreePort()
        val proxyClient = HttpProxy(
            url = host,
            port = port,
            proxyInterceptor = httpProxyInterceptor,
            requestCreator = ::RiotAuthRequest,
            responseCreator = { data: Body, url: String, headers: Headers, method: HttpMethod, status: HttpStatusCode? ->
                RiotAuthResponse(port, data, url, headers, method, status)
            })
        proxyClient
    }

    val xmppProxies = systemYamlPatcher.xmppHostsByRegion.map { (region, chat) ->
        val proxyClient = XmppProxy(chat.host, chat.port, xmppProxyInterceptor)
        val port = proxyClient.serverSocket.localAddress.port
        region to proxyClient
    }.toMap()

    val clientConfigProxy =
        HttpProxy(
            url = "https://clientconfig.rpg.riotgames.com",
            proxyInterceptor = httpProxyInterceptor,
            requestCreator = ::ConfigRequest,
            responseCreator = { data: Body, url: String, headers: Headers, method: HttpMethod, status: HttpStatusCode? ->
                ConfigResponse(
                    body = data,
                    url = url,
                    headers = headers,
                    method = method,
                    statusCode = status,
                    xmppProxies,
                    rmsProxies,
                    redEdgeProxies,
                    rioAuthProxy,
                    rioAuthenticateProxy,
                    rioEntitlementAuthProxy,
                    rioAffinityProxy
                )
            }

        )

    val lcdsHosts = rtmpProxies.map { (region, proxyClient) ->
        region to Host("127.0.0.1", proxyClient.serverSocket.localAddress.port)
    }.toMap()

    systemYamlPatcher.patchSystemYaml(lcdsHosts)

    val proxies =
        xmppProxies.values + rtmpProxies.values + rmsProxies + redEdgeProxies + rioAuthProxy + rioEntitlementAuthProxy + rioAffinityProxy + rioAuthenticateProxy

    return ClientProxy(
        systemYamlPatcher = systemYamlPatcher,
        clientConfigProxy = clientConfigProxy,
        proxies = proxies.toSet(),
        onClientClose = onClientClose
    )
}

class ClientProxy internal constructor(
    private val systemYamlPatcher: SystemYamlPatcher,
    private val clientConfigProxy: HttpProxy,
    val proxies: Set<Proxy>,
    val onClientClose: () -> Unit,
) : KoinComponent, AutoCloseable {

    suspend fun startProxies() = coroutineScope {
        proxies.forEach {
            launch {
                println("Started proxy for ${it.url} on port ${it.port}")
                it.start()
            }
        }

        clientConfigProxy.start()
        println("Started clientConfigProxy on port ${clientConfigProxy.port}")
    }

    suspend fun startClient(): Unit = coroutineScope {
        proxies.map(Proxy::started).joinAll()
        clientConfigProxy.started.join()
        launch(Dispatchers.IO) {
            process(
                systemYamlPatcher.riotClientPath,
                "--launch-product=league_of_legends",
                "--launch-patchline=live",
                "--client-config-url=\"http://127.0.0.1:${clientConfigProxy.port}\"",
                "--disable-patching"
            )
        }
    }

    override fun close() {
        systemYamlPatcher.close()
        runBlocking { proxies.map { launch { it.close() } }.joinAll() }
        clientConfigProxy.close()
        onClientClose()
    }
}
