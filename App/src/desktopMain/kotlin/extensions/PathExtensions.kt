package extensions

import okio.Path

fun Path.lastModified() = toFile().lastModified()