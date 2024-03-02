package proxies.interceptors

import rtmp.amf0.Amf0Node
import simpleJson.JsonNode

sealed interface Call {
    sealed interface ConfigCall : Call {
        val data: JsonNode

        data class ConfigResponse(override val data: JsonNode) : ConfigCall
    }

    sealed interface RtmpCall : Call {
        val data: List<Amf0Node>

        data class RtmpRequest(override val data: List<Amf0Node>) : RtmpCall
        data class RtmpResponse(override val data: List<Amf0Node>) : RtmpCall
    }
}