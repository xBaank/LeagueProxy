import client.SystemYamlPatcher
import io.ktor.client.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.cookies.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import shared.proxies.interceptors.HttpProxyInterceptor
import shared.proxies.interceptors.RmsProxyInterceptor
import shared.proxies.interceptors.RtmpProxyInterceptor
import shared.proxies.interceptors.XmppProxyInterceptor

val module = module {
    single {
        val yamlOptions = DumperOptions().apply { defaultFlowStyle = DumperOptions.FlowStyle.BLOCK }
        Yaml(yamlOptions)
    }
    single {
        HttpClient {
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            install(ContentEncoding) {
                gzip()
            }
        }
    }
    singleOf(::SettingsManager)
    singleOf(::RtmpProxyInterceptor)
    singleOf(::XmppProxyInterceptor)
    singleOf(::RmsProxyInterceptor)
    singleOf(::HttpProxyInterceptor)
    singleOf(::SystemYamlPatcher)
    singleOf(::SettingsManager)
}