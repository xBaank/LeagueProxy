package proxies.interceptors

import simpleJson.JsonNode

sealed interface Body {
    data class Json(val data: JsonNode)
    data class Text(val data: String)
    class Raw(val data: ByteArray)
}