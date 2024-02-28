package client

import com.github.pgreze.process.process
import extensions.port
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import proxies.RtmpProxy
import proxies.interceptors.RTMPProxyInterceptor

class ClientProxy(
    private val systemYamlPatcher: SystemYamlPatcher,
    val onClientClose: () -> Unit
) : KoinComponent, AutoCloseable {

    private val interceptor by inject<RTMPProxyInterceptor>()

    suspend fun startProxies() = coroutineScope {
        val proxies = systemYamlPatcher.lcdsHosts.map { (region, lcds) ->
            val proxyClient = RtmpProxy(lcds.host, lcds.port, interceptor)
            val port = proxyClient.serverSocket.localAddress.port
            println("Created proxy for $region on port $port")
            region to proxyClient
        }

        val lcdsHosts = proxies.associate { (region, proxyClient) ->
            region to LcdsHost("127.0.0.1", proxyClient.serverSocket.localAddress.port)
        }

        systemYamlPatcher.patchSystemYaml(lcdsHosts)

        proxies.onEach { (region, proxyClient) ->
            println("Started proxy for $region on port ${proxyClient.serverSocket.localAddress.port}")
            launch { proxyClient.start() }
        }
    }

    suspend fun startClient(): Unit = coroutineScope {
        launch(Dispatchers.IO) {
            process(
                systemYamlPatcher.riotClientPath,
                "--launch-product=league_of_legends",
                "--launch-patchline=live",
                "--disable-patching"
            )
        }
    }

    override fun close() {
        systemYamlPatcher.close()
        onClientClose()
    }
}
