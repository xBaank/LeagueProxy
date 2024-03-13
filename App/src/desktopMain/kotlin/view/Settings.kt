package view

import SettingsManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import org.koin.compose.koinInject

@Composable
fun Settings(
    isSettings: MutableState<Boolean>,
) {
    var showFilePicker by remember { mutableStateOf(false) }
    val settingsManager = koinInject<SettingsManager>()
    var settings by remember { mutableStateOf(settingsManager.settings.value) }

    LaunchedEffect(Unit) {
        settingsManager.settings.collect { settings = it }
    }


    val fileType = listOf("kts")
    FilePicker(show = showFilePicker, fileExtensions = fileType) { platformFile ->
        showFilePicker = false
        val value = platformFile?.path
        if (value != null) settingsManager.settings.value = settings.copy(scriptFile = value)
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxHeight().width(350.dp)) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            val rowWidth = 200.dp
            Row(
                modifier = Modifier.width(rowWidth),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Dark mode", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                Switch(
                    checked = settings.isDarkMode,
                    onCheckedChange = { settingsManager.settings.value = settings.copy(isDarkMode = it) },
                    modifier = Modifier.padding(horizontal = 16.dp).pointerHoverIcon(PointerIcon.Hand)
                )
            }
            Row(
                modifier = Modifier.width(rowWidth),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Collect", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                Switch(
                    checked = settings.isCollecting,
                    onCheckedChange = { settingsManager.settings.value = settings.copy(isCollecting = it) },
                    modifier = Modifier.padding(horizontal = 16.dp).pointerHoverIcon(PointerIcon.Hand)
                )
            }
            Row(
                modifier = Modifier.width(rowWidth),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Script", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                if (settings.scriptFile == null) {
                    Button(
                        onClick = { showFilePicker = true },
                        modifier = Modifier.padding(horizontal = 16.dp).pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Text("Select")
                    }
                } else {
                    Button(
                        onClick = { settingsManager.settings.value = settings.copy(scriptFile = null) },
                        modifier = Modifier.padding(horizontal = 16.dp).pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Text("Remove")
                    }
                }
            }
            Button(
                onClick = { isSettings.value = false },
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand).width(rowWidth).padding(top = 20.dp)
            ) {
                Text("Go back")
            }
        }
    }
}