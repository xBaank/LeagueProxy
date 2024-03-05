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
    val rmsHostsByRegion: Set<Host>
    val redEdgeHostsByRegion: Set<Host>
    private val systemYamlContentCopy: Map<String, Any>

    init {
        val (riotClientPath, lolPath) = getLolPaths()
        this.lolPath = lolPath
        this.riotClientPath = riotClientPath
        val (systemYamlPath, systemYamlContent) = getSystemYaml(lolPath)
        val (_, systemYamlContentCopy) = getSystemYaml(lolPath)
        this.systemYamlContent = systemYamlContent
        this.systemYamlContentCopy = systemYamlContentCopy

        this.rtmpHostsByRegion = getRtmpHosts()
        this.xmppHostsByRegion = getXmppHosts()
        this.rmsHostsByRegion = getRmsHosts()
        this.redEdgeHostsByRegion = getRedEdgeHosts()

        this.systemYamlPath = systemYamlPath

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
        rtmpHosts: Map<String, Host>,
    ) {
        systemYamlContent.getMap("region_data").forEach {
            val region = it.key
            if (rtmpHosts.containsKey(region)) {
                val lcds = it.value.getMap("servers").getMap("lcds") as MutableMap<String, Any?>
                lcds["lcds_host"] = rtmpHosts[region]!!.host
                lcds["lcds_port"] = rtmpHosts[region]!!.port
                lcds["use_tls"] = false
            }
        }

        FileSystem.SYSTEM.sink(systemYamlPath).buffer().use { it.writeUtf8(yaml.dump(systemYamlContent)) }
    }

    private fun getRtmpHosts(): Map<String, Host> {
        val hosts = mutableMapOf<String, Host>()
        systemYamlContent.getMap("region_data").forEach {
            val region = it.key
            val lcds = it.value.getMap("servers").getMap("lcds") as MutableMap<String, Any?>
            val host = lcds["lcds_host"] as String
            val port = lcds["lcds_port"] as Int
            hosts[region] = Host(host, port)
        }
        return hosts
    }

    private fun getRedEdgeHosts(): MutableSet<Host> {
        val hosts = mutableSetOf<Host>()
        systemYamlContent.getMap("region_data").forEach {
            val chat = it.value.getMap("servers").getMap("league_edge") as MutableMap<String, Any?>
            val host = chat["league_edge_url"] as String
            val port = 443
            hosts += Host(host, port)
        }
        return hosts
    }

    private fun getRmsHosts(): Set<Host> {
        val hosts = mutableSetOf<Host>()
        systemYamlContent.getMap("region_data").forEach {
            val chat = it.value.getMap("servers").getMap("rms") as MutableMap<String, Any?>
            val host = chat["rms_url"] as String
            val port = 443
            hosts += Host(host, port)
        }
        return hosts
    }

    private fun getXmppHosts(): Map<String, Host> {
        val hosts = mutableMapOf<String, Host>()
        systemYamlContent.getMap("region_data").forEach {
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