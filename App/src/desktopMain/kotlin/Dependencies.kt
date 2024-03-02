import client.SystemYamlPatcher
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import proxies.interceptors.ConfigProxyInterceptor
import proxies.interceptors.RTMPProxyInterceptor

val module = module {
    single {
        val yamlOptions = DumperOptions().apply { defaultFlowStyle = DumperOptions.FlowStyle.BLOCK }
        Yaml(yamlOptions)
    }
    singleOf(::RTMPProxyInterceptor)
    singleOf(::ConfigProxyInterceptor)
    singleOf(::SystemYamlPatcher)
}