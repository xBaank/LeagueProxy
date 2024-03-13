import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import shared.extensions.getOrThrow
import simpleJson.*

data class SettingsData(
    val scriptFile: String? = null,
    val isCollecting: Boolean = true,
    val isDarkMode: Boolean = true,
)

class SettingsManager {
    val settings = MutableStateFlow<SettingsData>(readFile())

    suspend fun collect() = coroutineScope {
        launch(Dispatchers.IO) {
            settings.collect {
                writeToFile(it)
            }
        }
    }

    private fun readFile(): SettingsData {
        val filePath = "./settings.json".toPath(true)
        val exists = FileSystem.SYSTEM.exists(filePath)
        if (!exists) writeToFile(SettingsData())

        return FileSystem.SYSTEM.read(filePath) {
            val data = readUtf8().deserialized().getOrThrow()
            SettingsData(
                scriptFile = data["scriptFile"].asString().getOrNull(),
                isCollecting = data["isCollecting"].asBoolean().getOrThrow(),
                isDarkMode = data["isDarkMode"].asBoolean().getOrThrow()
            )
        }
    }

    private fun writeToFile(settings: SettingsData) {
        val filePath = "./settings.json".toPath(true)

        FileSystem.SYSTEM.write(filePath) {
            val serialized = jObject {
                "scriptFile" += settings.scriptFile
                "isCollecting" += settings.isCollecting
                "isDarkMode" += settings.isDarkMode
            }.serialized()

            writeUtf8(serialized)
        }
    }

}