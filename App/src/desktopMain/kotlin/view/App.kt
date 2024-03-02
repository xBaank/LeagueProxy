package view

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ApplicationScope
import kotlinx.coroutines.runBlocking
import proxies.utils.killRiotClient
import view.theme.DarkColors
import view.theme.LightColors

@Composable
fun ApplicationScope.App(isRiotClientClosed: MutableState<Boolean>) {
    val isDarkColor = remember { mutableStateOf(true) }

    MaterialTheme(colorScheme = if (isDarkColor.value) DarkColors else LightColors) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            if (!isRiotClientClosed.value) {
                AlertDialog(
                    onDismissRequest = ::exitApplication,
                    onConfirmation = {
                        runBlocking { killRiotClient() }
                        isRiotClientClosed.value = true
                    },
                    dialogTitle = "Riot Client is already running",
                    dialogText = "Do you want to close it? If you dont close it LeagueProxy won't be launched"
                )
                return@Surface
            }


            ProxyCalls(isDarkColor)
        }
    }
}