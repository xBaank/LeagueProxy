package extensions

import io.ktor.http.*

fun Headers.isJson() = this[HttpHeaders.ContentType]?.contains("application/json") == true
fun Headers.isGzip() = this[HttpHeaders.ContentEncoding].equals("gzip", true)

fun Headers.isText() = this[HttpHeaders.ContentType]?.contains("application/jwt") == true
        || this[HttpHeaders.ContentType]?.contains("application/xml") == true
        || this[HttpHeaders.ContentType]?.contains("text/plain") == true
