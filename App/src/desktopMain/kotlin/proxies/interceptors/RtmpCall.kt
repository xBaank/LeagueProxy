package proxies.interceptors

import rtmp.amf0.Amf0Node

sealed interface RtmpCall {
    val data: List<Amf0Node>

    data class RtmpRequest(override val data: List<Amf0Node>) : RtmpCall
    data class RtmpResponse(override val data: List<Amf0Node>) : RtmpCall
}