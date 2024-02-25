import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import proxies.interceptors.RTMPProxyInterceptor

val module = module {
    singleOf(::RTMPProxyInterceptor)
    single {
        val yamlOptions = DumperOptions().apply { defaultFlowStyle = DumperOptions.FlowStyle.BLOCK }
        Yaml(yamlOptions)
    }
}