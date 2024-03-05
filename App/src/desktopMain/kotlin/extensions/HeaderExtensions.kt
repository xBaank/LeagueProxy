package extensions

import io.ktor.http.*

fun Headers.isJson() = this[HttpHeaders.ContentType]?.contains("application/json") == true