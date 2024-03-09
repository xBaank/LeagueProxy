import client.SystemYamlPatcher
import io.ktor.client.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.cookies.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import proxies.interceptors.HttpProxyInterceptor
import proxies.interceptors.RmsProxyInterceptor
import proxies.interceptors.RtmpProxyInterceptor
import proxies.interceptors.XmppProxyInterceptor
import view.SettingsManager

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
    singleOf(::RtmpProxyInterceptor)
    singleOf(::XmppProxyInterceptor)
    singleOf(::RmsProxyInterceptor)
    singleOf(::HttpProxyInterceptor)
    singleOf(::SystemYamlPatcher)
    singleOf(::SettingsManager)
}