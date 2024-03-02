package client

import com.github.pgreze.process.process
import extensions.inject
import extensions.port
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import proxies.ClientConfigProxy
import proxies.RtmpProxy
import proxies.XmppProxy
import proxies.interceptors.ConfigProxyInterceptor
import proxies.interceptors.RtmpProxyInterceptor
import proxies.interceptors.XmppProxyInterceptor

fun CreateClientProxy(systemYamlPatcher: SystemYamlPatcher, onClientClose: () -> Unit): ClientProxy {
    val rtmpProxyInterceptor by inject<RtmpProxyInterceptor>()
    val configProxyInterceptor by inject<ConfigProxyInterceptor>()
    val xmppProxyInterceptor by inject<XmppProxyInterceptor>()

    val rtmpProxies = systemYamlPatcher.rtmpHostsByRegion.map { (region, lcds) ->
        val proxyClient = RtmpProxy(lcds.host, lcds.port, rtmpProxyInterceptor)
        val port = proxyClient.serverSocket.localAddress.port
        println("Created rtmp proxy for $region on port $port")
        region to proxyClient
    }

    val xmppProxies = systemYamlPatcher.xmppHostsByRegion.map { (region, chat) ->
        val proxyClient = XmppProxy(chat.host, chat.port, xmppProxyInterceptor)
        val port = proxyClient.serverSocket.localAddress.port
        println("Created xmpp proxy for $region on port $port")
        region to proxyClient
    }

    val clientConfigProxy = ClientConfigProxy(configProxyInterceptor, xmppProxies.toMap())

    val lcdsHosts = rtmpProxies.associate { (region, proxyClient) ->
        region to Host("127.0.0.1", proxyClient.serverSocket.localAddress.port)
    }

    systemYamlPatcher.patchSystemYaml(lcdsHosts)

    return ClientProxy(systemYamlPatcher, clientConfigProxy, rtmpProxies.toMap(), xmppProxies.toMap(), onClientClose)
}

class ClientProxy internal constructor(
    private val systemYamlPatcher: SystemYamlPatcher,
    private val clientConfigProxy: ClientConfigProxy,
    private val rtmpProxies: Map<String, RtmpProxy>,
    private val xmppProxies: Map<String, XmppProxy>,
    val onClientClose: () -> Unit,
) : KoinComponent, AutoCloseable {

    private val configServerStarted: CompletableJob = Job()

    suspend fun startProxies() = coroutineScope {
        rtmpProxies.onEach { (region, proxyClient) ->
            println("Started rtmp proxy for $region on port ${proxyClient.serverSocket.localAddress.port}")
            launch { proxyClient.start() }
        }

        xmppProxies.onEach { (region, proxyClient) ->
            println("Started xmpp proxy for $region on port ${proxyClient.serverSocket.localAddress.port}")
            launch { proxyClient.start() }
        }

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
        clientConfigProxy.close()
        systemYamlPatcher.close()
        onClientClose()
    }
}
