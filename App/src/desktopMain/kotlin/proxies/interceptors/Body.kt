package proxies.interceptors

import simpleJson.JsonNode

sealed interface Body {
    data class Json(var data: JsonNode) : Body
    data class Text(var data: String) : Body
    class Raw(var data: ByteArray) : Body
}