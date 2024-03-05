package client

import com.github.pgreze.process.process
import extensions.inject
import extensions.port
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import proxies.*
import proxies.interceptors.Call.RedEdgeCall.RedEdgeRequest
import proxies.interceptors.Call.RedEdgeCall.RedEdgeResponse
import proxies.interceptors.Call.RiotAuthCall.RiotAuthRequest
import proxies.interceptors.Call.RiotAuthCall.RiotAuthResponse
import proxies.interceptors.HttpProxyInterceptor
import proxies.interceptors.RmsProxyInterceptor
import proxies.interceptors.RtmpProxyInterceptor
import proxies.interceptors.XmppProxyInterceptor

fun CreateClientProxy(systemYamlPatcher: SystemYamlPatcher, onClientClose: () -> Unit): ClientProxy {
    val rtmpProxyInterceptor by inject<RtmpProxyInterceptor>()
    val xmppProxyInterceptor by inject<XmppProxyInterceptor>()
    val rmsProxyInterceptor by inject<RmsProxyInterceptor>()
    val httpProxyInterceptor by inject<HttpProxyInterceptor>()

    val rtmpProxies = systemYamlPatcher.rtmpHostsByRegion.map { (region, lcds) ->
        val proxyClient = RtmpProxy(lcds.host, lcds.port, rtmpProxyInterceptor)
        val port = proxyClient.serverSocket.localAddress.port
        println("Created rtmp proxy for $region on port $port")
        region to proxyClient
    }

    val rmsProxies = systemYamlPatcher.rmsHostsByRegion.map { host ->
        val proxyClient = RmsProxy(host.host, rmsProxyInterceptor)
        println("Created rms proxy for ${host.host}")
        proxyClient
    }.toSet()

    val redEdgeProxies = systemYamlPatcher.redEdgeHostsByRegion.map { host ->
        val proxyClient = HttpProxy(host.host, httpProxyInterceptor, ::RedEdgeRequest, ::RedEdgeResponse)
        println("Created red edge proxy for ${host.host}")
        proxyClient
    }.toSet()

    val rioAuthenticateProxy = run {
        val host = "https://authenticate.riotgames.com"
        val proxyClient = HttpProxy(host, httpProxyInterceptor, ::RiotAuthRequest, ::RiotAuthResponse)
        println("Created riot authenticate proxy for $host")
        proxyClient
    }

    val rioAuthProxy = run {
        val host = "https://auth.riotgames.com"
        val proxyClient = HttpProxy(host, httpProxyInterceptor, ::RiotAuthRequest, ::RiotAuthResponse)
        println("Created riot auth proxy for $host")
        proxyClient
    }

    val rioAffinityProxy = run {
        val host = "https://riot-geo.pas.si.riotgames.com"
        val proxyClient = HttpProxy(host, httpProxyInterceptor, ::RiotAuthRequest, ::RiotAuthResponse)
        println("Created riot affinity proxy for $host")
        proxyClient
    }

    val rioEntitlementAuthProxy = run {
        val host = "https://entitlements.auth.riotgames.com/api/token/v1"
        val proxyClient = HttpProxy(host, httpProxyInterceptor, ::RiotAuthRequest, ::RiotAuthResponse)
        println("Created riot auth proxy for $host")
        proxyClient
    }

    val xmppProxies = systemYamlPatcher.xmppHostsByRegion.map { (region, chat) ->
        val proxyClient = XmppProxy(chat.host, chat.port, xmppProxyInterceptor)
        val port = proxyClient.serverSocket.localAddress.port
        println("Created xmpp proxy for $region on port $port")
        region to proxyClient
    }

    val clientConfigProxy =
        ClientConfigProxy(
            httpProxyInterceptor,
            xmppProxies.toMap(),
            rmsProxies,
            redEdgeProxies,
            rioAuthProxy,
            rioAuthenticateProxy,
            rioEntitlementAuthProxy,
            rioAffinityProxy
        )

    val lcdsHosts = rtmpProxies.associate { (region, proxyClient) ->
        region to Host("127.0.0.1", proxyClient.serverSocket.localAddress.port)
    }

    systemYamlPatcher.patchSystemYaml(lcdsHosts)

    return ClientProxy(
        systemYamlPatcher = systemYamlPatcher,
        clientConfigProxy = clientConfigProxy,
        rtmpProxies = rtmpProxies.toMap(),
        xmppProxies = xmppProxies.toMap(),
        rmsProxies = rmsProxies,
        redEdgeProxies = redEdgeProxies,
        riotAuthProxy = rioAuthProxy,
        riotAuthenticateProxy = rioAuthenticateProxy,
        riotEntitlementAuthProxy = rioEntitlementAuthProxy,
        riotAffinityProxy = rioAffinityProxy,
        onClientClose = onClientClose
    )
}

class ClientProxy internal constructor(
    private val systemYamlPatcher: SystemYamlPatcher,
    private val clientConfigProxy: ClientConfigProxy,
    private val rtmpProxies: Map<String, RtmpProxy>,
    private val xmppProxies: Map<String, XmppProxy>,
    private val rmsProxies: Set<RmsProxy>,
    private val redEdgeProxies: Set<HttpProxy>,
    private val riotAuthProxy: HttpProxy,
    private val riotEntitlementAuthProxy: HttpProxy,
    private val riotAuthenticateProxy: HttpProxy,
    private val riotAffinityProxy: HttpProxy,
    val onClientClose: () -> Unit,
) : KoinComponent, AutoCloseable {

    private val configServerStarted: CompletableJob = Job()

    suspend fun startProxies() = coroutineScope {
        rtmpProxies.onEach { (region, proxyClient) ->
            println("Started rtmp proxy for $region on port ${proxyClient.serverSocket.localAddress.port}")
            launch { proxyClient.start() }
        }

        rmsProxies.onEach { proxyClient ->
            println("Started rms proxy for ${proxyClient.url} on port ${proxyClient.port}")
            launch { proxyClient.start() }
        }

        redEdgeProxies.onEach { proxyClient ->
            println("Started red edge proxy for ${proxyClient.url} on port ${proxyClient.port}")
            launch { proxyClient.start() }
        }

        xmppProxies.onEach { (region, proxyClient) ->
            println("Started xmpp proxy for $region on port ${proxyClient.serverSocket.localAddress.port}")
            launch { proxyClient.start() }
        }

        /*        println("Started riot auth proxy for ${riotAuthProxy.url} on port ${riotAuthProxy.port}")
                riotAuthProxy.start()

                println("Started riot authenticate proxy for ${riotAuthenticateProxy.url} on port ${riotAuthenticateProxy.port}")
                riotAuthenticateProxy.start()*/

        println("Started riot entitlement auth proxy for ${riotEntitlementAuthProxy.url} on port ${riotEntitlementAuthProxy.port}")
        riotEntitlementAuthProxy.start()

        println("Started riot entitlement auth proxy for ${riotAffinityProxy.url} on port ${riotAffinityProxy.port}")
        riotAffinityProxy.start()

        clientConfigProxy.start()
        configServerStarted.complete()
        println("Started clientConfigProxy on port ${clientConfigProxy.port}")
    }

    suspend fun startClient(): Unit = coroutineScope {
        configServerStarted.join()
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
        rmsProxies.forEach { it.close() }
        clientConfigProxy.close()
        systemYamlPatcher.close()
        onClientClose()
    }
}
