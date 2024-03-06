package extensions

import io.github.reactivecircus.cache4k.Cache
import simpleJson.JsonNode
import simpleJson.serialized
import simpleJson.serializedPretty
import kotlin.time.Duration.Companion.minutes

private val map = Cache.Builder<JsonNode, String>()
    .expireAfterAccess(5.minutes)
    .build()

private val prettyMap = Cache.Builder<JsonNode, String>()
    .expireAfterAccess(5.minutes)
    .build()

private val all = Cache.Builder<Any, String>()
    .expireAfterAccess(5.minutes)
    .build()

private inline fun <T : Any, V : Any> Cache<T, V>.getOrPut(key: T, factory: () -> V): V {
    val cachedValue = get(key)
    if (cachedValue != null) return cachedValue
    val value = factory()
    put(key, value)
    return value
}

fun JsonNode.serializedPrettyMemo(): String = prettyMap.getOrPut(this, ::serializedPretty)

fun JsonNode.serializedMemo(): String = map.getOrPut(this, ::serialized)

fun Any.serializedMemo(): String = all.getOrPut(this, ::prettyPrint)

