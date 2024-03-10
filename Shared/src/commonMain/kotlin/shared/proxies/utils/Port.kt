package shared.proxies.utils

import java.net.ServerSocket

fun findFreePort(): Int {
    var port = -1
    try {
        val socket = ServerSocket(0)
        port = socket.localPort
        socket.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return port
}
