package client

import com.github.pgreze.process.process
import extensions.inject
import extensions.port
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import proxies.ClientConfigProxy
import proxies.RtmpProxy
import proxies.interceptors.ConfigProxyInterceptor
import proxies.interceptors.RTMPProxyInterceptor

fun CreateClientProxy(systemYamlPatcher: SystemYamlPatcher, onClientClose: () -> Unit): ClientProxy {
    val rtmpProxyInterceptor by inject<RTMPProxyInterceptor>()
    val configProxyInterceptor by inject<ConfigProxyInterceptor>()

    val proxies = systemYamlPatcher.lcdsHosts.map { (region, lcds) ->
        val proxyClient = RtmpProxy(lcds.host, lcds.port, rtmpProxyInterceptor)
        val port = proxyClient.serverSocket.localAddress.port
        println("Created rtmp proxy for $region on port $port")
        region to proxyClient
    }

    val clientConfigProxy = ClientConfigProxy(configProxyInterceptor)

    val lcdsHosts = proxies.associate { (region, proxyClient) ->
        region to LcdsHost("127.0.0.1", proxyClient.serverSocket.localAddress.port)
    }

    systemYamlPatcher.patchSystemYaml(lcdsHosts)

    return ClientProxy(systemYamlPatcher, clientConfigProxy, proxies.toMap(), onClientClose)
}

class ClientProxy internal constructor(
    private val systemYamlPatcher: SystemYamlPatcher,
    private val clientConfigProxy: ClientConfigProxy,
    private val rtmpProxies: Map<String, RtmpProxy>,
    val onClientClose: () -> Unit,
) : KoinComponent, AutoCloseable {

    suspend fun startProxies() = coroutineScope {
        rtmpProxies.onEach { (region, proxyClient) ->
            println("Started rtmp proxy for $region on port ${proxyClient.serverSocket.localAddress.port}")
            launch { proxyClient.start() }
        }

        clientConfigProxy.start()
        println("Started clientConfigProxy on port ${clientConfigProxy.port}")
    }

    suspend fun startClient(): Unit = coroutineScope {
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
        onClientClose()
    }
}
