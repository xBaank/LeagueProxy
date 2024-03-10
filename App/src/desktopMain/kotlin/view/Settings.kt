package view

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

@Composable
fun Settings(isDarkColors: MutableState<Boolean>, isSettings: MutableState<Boolean>) {
    var showFilePicker by remember { mutableStateOf(false) }

    val fileType = listOf("main.kts")
    FilePicker(show = showFilePicker, fileExtensions = fileType) { platformFile ->
        showFilePicker = false
        // do something with the file
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Dark mode", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                Switch(
                    checked = isDarkColors.value,
                    onCheckedChange = { isDarkColors.value = it },
                    modifier = Modifier.padding(16.dp).pointerHoverIcon(PointerIcon.Hand)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Intercept script", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                Button(
                    onClick = { showFilePicker = true },
                    modifier = Modifier.padding(16.dp).pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Text("Select")
                }
            }
            Button(onClick = { isSettings.value = false }, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                Text("Save")
            }
        }
    }
}