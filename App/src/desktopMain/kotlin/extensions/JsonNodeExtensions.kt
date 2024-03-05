package extensions

import simpleJson.JsonNode
import simpleJson.serialized

val map = mutableMapOf<JsonNode, String>()
fun JsonNode.serializedMemo() = map.getOrPut(this) { this.serialized() }