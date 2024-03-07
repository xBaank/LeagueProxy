package extensions

import io.github.reactivecircus.cache4k.Cache
import simpleJson.JsonNode
import simpleJson.serialized
import simpleJson.serializedPretty
import kotlin.time.Duration.Companion.seconds

private val mapCutted = Cache.Builder<JsonNode, String>()
    .expireAfterAccess(10.seconds)
    .build()

private val prettyMapCutted = Cache.Builder<JsonNode, String>()
    .expireAfterAccess(10.seconds)
    .build()

private val all = Cache.Builder<Any, String>()
    .expireAfterAccess(10.seconds)
    .build()

private inline fun <T : Any, V : Any> Cache<T, V>.getOrPut(key: T, factory: () -> V): V {
    val cachedValue = get(key)
    if (cachedValue != null) return cachedValue
    val value = factory()
    put(key, value)
    return value
}

fun JsonNode.serializedPrettyMemoCutted(): String = prettyMapCutted.getOrPut(this) {
    val value = serializedPretty()
    value.substring(0..minOf(value.length - 1, 100_000))
}

fun JsonNode.serializedMemoCutted(): String = mapCutted.getOrPut(this) {
    val value = serialized()
    serialized().substring(0..minOf(value.length - 1, 100_000))
}

fun Any.serializedMemo(): String = all.getOrPut(this, ::prettyPrint)

