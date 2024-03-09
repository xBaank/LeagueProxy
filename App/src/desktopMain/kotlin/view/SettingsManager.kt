package view

import kotlinx.coroutines.flow.MutableStateFlow

class SettingsManager {
    var showSettings = MutableStateFlow(false)
}