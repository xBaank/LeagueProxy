@file:Suppress("UNCHECKED_CAST")

package proxies.extensions

fun Any?.getMap(s: String) = (this as Map<String, Any?>)[s] as Map<String, Any?>
