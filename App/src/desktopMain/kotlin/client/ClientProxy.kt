package client

import com.github.pgreze.process.process
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import shared.Body
import shared.Call.ConfigCall.ConfigRequest
import shared.Call.ConfigCall.ConfigResponse
import shared.Call.GenericHttpCall.GenericHttpRequest
import shared.Call.GenericHttpCall.GenericHttpResponse
import shared.Call.RiotAuthCall.RiotAuthRequest
import shared.Call.RiotAuthCall.RiotAuthResponse
import shared.extensions.inject
import shared.extensions.port
import shared.proxies.*
import shared.proxies.interceptors.HttpProxyInterceptor
import shared.proxies.interceptors.RmsProxyInterceptor
import shared.proxies.interceptors.RtmpProxyInterceptor
import shared.proxies.interceptors.XmppProxyInterceptor
import shared.proxies.utils.findFreePort

private val logger = KotlinLogging.logger {}

fun CreateClientProxy(systemYamlPatcher: SystemYamlPatcher, onClientClose: () -> Unit): ClientProxy {
    val rtmpProxyInterceptor by inject<RtmpProxyInterceptor>()
    val xmppProxyInterceptor by inject<XmppProxyInterceptor>()
    val rmsProxyInterceptor by inject<RmsProxyInterceptor>()
    val httpProxyInterceptor by inject<HttpProxyInterceptor>()

    val rtmpProxies = systemYamlPatcher.rtmpHostsByRegion.map { (region, lcds) ->
        val proxyClient = RtmpProxy(lcds.host, lcds.port, rtmpProxyInterceptor)
        region to proxyClient
    }.toMap()

    val rmsProxies = systemYamlPatcher.rmsHostsByRegion.map { (region, host) ->
        val proxyClient = RmsProxy(host.host, rmsProxyInterceptor)
        region to proxyClient
    }.toMap()

    val redEdgeProxies = systemYamlPatcher.redEdgeHostsByRegion.map { (region, host) ->
        val proxyClient = HttpProxy(host.host, httpProxyInterceptor, ::GenericHttpRequest, ::GenericHttpResponse)
        region to proxyClient
    }.toMap()

    val rioAuthenticateProxy = run {
        val port = findFreePort()
        val proxyClient = HttpProxy(
            port = port,
            proxyInterceptor = httpProxyInterceptor,
            requestCreator = ::RiotAuthRequest,
            responseCreator = { data: Body, url: String, headers: Headers, method: HttpMethod, status: HttpStatusCode? ->
                RiotAuthResponse(port, data, url, headers, method, status)
            })
        proxyClient
    }

    val rioAuthProxy = run {
        val port = findFreePort()
        val proxyClient = HttpProxy(
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
        val port = findFreePort()
        val proxyClient = HttpProxy(
            port = port,
            proxyInterceptor = httpProxyInterceptor,
            requestCreator = ::RiotAuthRequest,
            responseCreator = { data: Body, url: String, headers: Headers, method: HttpMethod, status: HttpStatusCode? ->
                RiotAuthResponse(port, data, url, headers, method, status)
            }
        )
        proxyClient
    }

    val riotPlatformEdge = run {
        val port = findFreePort()
        val proxyClient = HttpProxy(
            port = port,
            proxyInterceptor = httpProxyInterceptor,
            requestCreator = ::GenericHttpRequest,
            responseCreator = { data: Body, url: String, headers: Headers, method: HttpMethod, status: HttpStatusCode? ->
                GenericHttpResponse(data, url, headers, method, status)
            }
        )
        proxyClient
    }

    val riotReportCollector = run {
        val port = findFreePort()
        val proxyClient = HttpProxy(
            port = port,
            proxyInterceptor = httpProxyInterceptor,
            requestCreator = ::GenericHttpRequest,
            responseCreator = { data: Body, url: String, headers: Headers, method: HttpMethod, status: HttpStatusCode? ->
                GenericHttpResponse(data, url, headers, method, status)
            }
        )
        proxyClient
    }

    val rioEntitlementAuthProxy = run {
        val port = findFreePort()
        val proxyClient = HttpProxy(
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
                    xmppProxies = xmppProxies,
                    rmsProxies = rmsProxies.values.toSet(),
                    redEdgeProxies = redEdgeProxies.values.toSet(),
                    riotAuthProxy = rioAuthProxy,
                    riotAuthenticateProxy = rioAuthenticateProxy,
                    rioEntitlementAuthProxy = rioEntitlementAuthProxy,
                    riotAffinityServer = rioAffinityProxy,
                    riotPlatformEdge = riotPlatformEdge,
                    riotReportCollector = riotReportCollector
                )
            }

        )

    val lcdsHosts = rtmpProxies.map { (region, proxyClient) ->
        region to Host("127.0.0.1", proxyClient.serverSocket.localAddress.port)
    }.toMap()

    systemYamlPatcher.patchSystemYaml(lcdsHosts)

    val proxies = setOf<Proxy>(
        rioAuthProxy,
        rioEntitlementAuthProxy,
        rioAffinityProxy,
        rioAuthenticateProxy,
        riotPlatformEdge,
        riotReportCollector
    ) + xmppProxies.values + rtmpProxies.values + rmsProxies.values + redEdgeProxies.values

    return ClientProxy(
        systemYamlPatcher = systemYamlPatcher,
        clientConfigProxy = clientConfigProxy,
        proxies = proxies,
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
        proxies.forEach { launch { it.start() } }
        clientConfigProxy.start()
        logger.info { "Started clientConfigProxy on port ${clientConfigProxy.port}" }
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
