import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import proxies.interceptors.RTMPProxyInterceptor

val module = module {
    factoryOf(::RTMPProxyInterceptor)
    single {
        val yamlOptions = DumperOptions().apply { defaultFlowStyle = DumperOptions.FlowStyle.BLOCK }
        Yaml(yamlOptions)
    }
}