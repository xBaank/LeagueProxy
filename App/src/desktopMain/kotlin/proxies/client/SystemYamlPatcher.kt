package proxies.client

import arrow.core.getOrElse
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.yaml.snakeyaml.Yaml
import proxies.LeagueNotFoundException
import proxies.extensions.getMap
import simpleJson.asString
import simpleJson.deserialized
import simpleJson.get

class SystemYamlPatcher(
    val proxyHosts: Map<String, LcdsHost>
) : KoinComponent, AutoCloseable {
    val yaml by inject<Yaml>()

    val riotClientPath: String
    val lolPath: String
    val systemYamlPath: Path
    val systemYamlContent: Map<String, Any>
    val lcdsHosts: Map<String, LcdsHost>

    init {
        val (riotClientPath, lolPath) = getLolPaths()
        this.lolPath = lolPath
        this.riotClientPath = riotClientPath

        val hosts = getHosts()
        this.lcdsHosts = hosts

        val (systemYamlPath, systemYamlContent) = patchSystemYaml(lolPath, proxyHosts)
        this.systemYamlPath = systemYamlPath
        this.systemYamlContent = systemYamlContent
    }

    private fun getLolPaths(): Pair<String, String> {
        val lolClientInstalls: Path = System.getenv("ALLUSERSPROFILE")
            ?.let { "$it/Riot Games/Metadata/league_of_legends.live/league_of_legends.live.product_settings.yaml" }
            ?.toPath(true)
            ?.takeIf { FileSystem.SYSTEM.exists(it) }
            ?: throw LeagueNotFoundException("Cannot find Lol Client Installs (ALLUSERSPROFILE)")

        val riotClientInstalls = System.getenv("ALLUSERSPROFILE")
            ?.let { "$it/Riot Games/RiotClientInstalls.json" }
            ?.toPath(true)
            ?.takeIf { FileSystem.SYSTEM.exists(it) }
            ?: throw LeagueNotFoundException("Cannot find Riot Client Installs (ALLUSERSPROFILE)")

        val riotClientInstallsJson = FileSystem.SYSTEM.source(riotClientInstalls).buffer().readUtf8().deserialized()
        val riotClientPath = riotClientInstallsJson["rc_live"].asString()
            .getOrElse { throw LeagueNotFoundException("Cannot find property rc_live") }

        val file = FileSystem.SYSTEM.source(lolClientInstalls).buffer()

        val yamlMap = yaml.load<Map<String, Any>>(file.readUtf8())
        val lolPath: String = yamlMap["product_install_full_path"] as String
        return Pair(riotClientPath, lolPath)
    }

    private fun patchSystemYaml(
        lolPath: String,
        hosts: Map<String, LcdsHost>
    ): Pair<Path, Map<String, Any>> {
        val systemYamlPath = lolPath.toPath(true).resolve("system.yaml")
        val systemYaml = FileSystem.SYSTEM.source(systemYamlPath).buffer().use { it.readUtf8() }
        val systemYamlMap = yaml.load<Map<String, Any>>(systemYaml)
        val systemYamlMapOriginal = yaml.load<Map<String, Any>>(systemYaml)

        systemYamlMap.getMap("region_data").forEach {
            val region = it.key
            if (!hosts.containsKey(region)) return@forEach
            val lcds = it.value.getMap("servers").getMap("lcds") as MutableMap<String, Any?>
            lcds["lcds_host"] = hosts[region]!!.host
            lcds["lcds_port"] = hosts[region]!!.port
            lcds["use_tls"] = false
        }

        FileSystem.SYSTEM.sink(systemYamlPath).buffer().use { it.writeUtf8(yaml.dump(systemYamlMap)) }
        return Pair(systemYamlPath, systemYamlMapOriginal)
    }

    private fun getHosts(): Map<String, LcdsHost> {
        val systemYamlPath = lolPath.toPath(true).resolve("system.yaml")
        val systemYaml = FileSystem.SYSTEM.source(systemYamlPath)
            .buffer()

        val systemYamlMap = systemYaml.use { yaml.load<Map<String, Any>>(systemYaml.readUtf8()) }
        val hosts = mutableMapOf<String, LcdsHost>()
        systemYamlMap.getMap("region_data").forEach {
            val region = it.key
            val lcds = it.value.getMap("servers").getMap("lcds") as MutableMap<String, Any?>
            val host = lcds["lcds_host"] as String
            val port = lcds["lcds_port"] as Int
            hosts[region] = LcdsHost(host, port)
        }
        return hosts
    }

    override fun close() {
        FileSystem.SYSTEM.sink(systemYamlPath).buffer().use { it.writeUtf8(yaml.dump(systemYamlContent)) }
    }
}