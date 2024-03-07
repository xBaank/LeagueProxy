package extensions

import proxies.interceptors.Body
import simpleJson.serialized

@OptIn(ExperimentalStdlibApi::class)
fun Body.serializedPrettyMemoCutted() = when (this) {
    is Body.Json -> data.serializedPrettyMemoCutted()
    is Body.Raw -> data.toHexString()
    is Body.Text -> data
}

@OptIn(ExperimentalStdlibApi::class)
fun Body.serialized() = when (this) {
    is Body.Json -> data.serialized()
    is Body.Raw -> data.toHexString()
    is Body.Text -> data
}

fun Body.isEmpty() = when (this) {
    is Body.Json -> false
    is Body.Raw -> data.isEmpty()
    is Body.Text -> data.isBlank()
}
