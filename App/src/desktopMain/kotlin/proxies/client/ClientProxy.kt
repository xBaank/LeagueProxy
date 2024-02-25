package proxies.client

import com.github.pgreze.process.process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import proxies.RtmpProxy
import proxies.extensions.port
import proxies.interceptors.RTMPProxyInterceptor

class ClientProxy(
    private val systemYamlPatcher: SystemYamlPatcher,
    val onClientClose: () -> Unit
) : KoinComponent, AutoCloseable {

    private val interceptor by inject<RTMPProxyInterceptor>()

    suspend fun startProxies() = coroutineScope {
        systemYamlPatcher.lcdsHosts.map { (region, lcds) ->
            val proxyClient = RtmpProxy(lcds.host, lcds.port, interceptor)
            val port = proxyClient.serverSocket.localAddress.port
            println("Created proxy for $region on port $port")
            proxyClient
        }.forEach {
            launch { it.start() }
        }
    }

    suspend fun startClient(): Unit = coroutineScope {
        launch(Dispatchers.IO) {
            process(
                systemYamlPatcher.riotClientPath,
                "--launch-product=league_of_legends",
                "--launch-patchline=live",
                "--disable-patching",
                destroyForcibly = true
            )
        }
    }

    override fun close() {
        systemYamlPatcher.close()
        onClientClose()
    }
}
