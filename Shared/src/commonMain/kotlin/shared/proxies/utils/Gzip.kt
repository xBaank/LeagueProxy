package shared.proxies.utils

import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

//TODO Can we use a multiplatform library for this?

fun String.base64Ungzip(): String {
    val gzipped: ByteArray = Base64.getDecoder().decode(this.toByteArray())
    val `in` = GZIPInputStream(gzipped.inputStream())
    return String(`in`.readBytes())
}

fun String.gzipBase64(): String {
    val outputStream = ByteArrayOutputStream()
    GZIPOutputStream(outputStream).bufferedWriter(Charsets.UTF_8).use { it.write(this); it.flush() }
    return Base64.getEncoder().encodeToString(outputStream.toByteArray())
}

fun ByteArray.ungzip(): String {
    val `in` = GZIPInputStream(this.inputStream())
    return String(`in`.readBytes())
}

fun String.gzip(): ByteArray {
    val outputStream = ByteArrayOutputStream()
    GZIPOutputStream(outputStream).bufferedWriter(Charsets.UTF_8).use { it.write(this); it.flush() }
    return outputStream.toByteArray()
}

fun ByteArray.gzipArray(): ByteArray {
    val outputStream = ByteArrayOutputStream()
    GZIPOutputStream(outputStream).use { it.write(this); it.flush() }
    return outputStream.toByteArray()
}