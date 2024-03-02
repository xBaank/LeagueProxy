package client

import arrow.core.getOrElse
import exceptions.LeagueNotFoundException
import extensions.getMap
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.yaml.snakeyaml.Yaml
import simpleJson.asString
import simpleJson.deserialized
import simpleJson.get

class SystemYamlPatcher : KoinComponent, AutoCloseable {
    val yaml by inject<Yaml>()

    val riotClientPath: String
    val lolPath: String
    val systemYamlPath: Path
    val systemYamlContent: Map<String, Any>
    val rtmpHostsByRegion: Map<String, Host>
    val xmppHostsByRegion: Map<String, Host>
    private val systemYamlContentCopy: Map<String, Any>

    init {
        val (riotClientPath, lolPath) = getLolPaths()
        this.lolPath = lolPath
        this.riotClientPath = riotClientPath

        this.rtmpHostsByRegion = getRtmpHosts()
        this.xmppHostsByRegion = getXmppHosts()

        val (systemYamlPath, systemYamlContent) = getSystemYaml(lolPath)
        val (_, systemYamlContentCopy) = getSystemYaml(lolPath)
        this.systemYamlPath = systemYamlPath
        this.systemYamlContent = systemYamlContent
        this.systemYamlContentCopy = systemYamlContentCopy
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

    private fun getSystemYaml(
        lolPath: String,
    ): Pair<Path, Map<String, Any>> {
        val systemYamlPath = lolPath.toPath(true).resolve("system.yaml")
        val systemYaml = FileSystem.SYSTEM.source(systemYamlPath).buffer().use { it.readUtf8() }
        val systemYamlMapOriginal = yaml.load<Map<String, Any>>(systemYaml)

        return Pair(systemYamlPath, systemYamlMapOriginal)
    }

    fun patchSystemYaml(
        hosts: Map<String, Host>,
    ) {
        systemYamlContent.getMap("region_data").forEach {
            val region = it.key
            if (!hosts.containsKey(region)) return@forEach
            val lcds = it.value.getMap("servers").getMap("lcds") as MutableMap<String, Any?>
            lcds["lcds_host"] = hosts[region]!!.host
            lcds["lcds_port"] = hosts[region]!!.port
            lcds["use_tls"] = false
        }

        FileSystem.SYSTEM.sink(systemYamlPath).buffer().use { it.writeUtf8(yaml.dump(systemYamlContent)) }
    }

    private fun getRtmpHosts(): Map<String, Host> {
        val systemYamlPath = lolPath.toPath(true).resolve("system.yaml")
        val systemYaml = FileSystem.SYSTEM.source(systemYamlPath)
            .buffer()

        val systemYamlMap = systemYaml.use { yaml.load<Map<String, Any>>(systemYaml.readUtf8()) }
        val hosts = mutableMapOf<String, Host>()
        systemYamlMap.getMap("region_data").forEach {
            val region = it.key
            val lcds = it.value.getMap("servers").getMap("lcds") as MutableMap<String, Any?>
            val host = lcds["lcds_host"] as String
            val port = lcds["lcds_port"] as Int
            hosts[region] = Host(host, port)
        }
        return hosts
    }

    private fun getXmppHosts(): Map<String, Host> {
        val systemYamlPath = lolPath.toPath(true).resolve("system.yaml")
        val systemYaml = FileSystem.SYSTEM.source(systemYamlPath)
            .buffer()

        val systemYamlMap = systemYaml.use { yaml.load<Map<String, Any>>(systemYaml.readUtf8()) }
        val hosts = mutableMapOf<String, Host>()
        systemYamlMap.getMap("region_data").forEach {
            val region = it.key
            val chat = it.value.getMap("servers").getMap("chat") as MutableMap<String, Any?>
            val host = chat["chat_host"] as String
            val port = chat["chat_port"] as Int
            hosts[region] = Host(host, port)
        }
        return hosts
    }

    override fun close() {
        FileSystem.SYSTEM.sink(systemYamlPath).buffer().use { it.writeUtf8(yaml.dump(systemYamlContentCopy)) }
    }
}