package view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Settings(isDarkColors: MutableState<Boolean>, isSettings: MutableState<Boolean>) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dark mode: ")
            Switch(
                checked = isDarkColors.value,
                onCheckedChange = { isDarkColors.value = it },
                modifier = Modifier.padding(16.dp)
            )
        }
        Button(onClick = { isSettings.value = false }) {
            Text("Save")
        }
    }
}