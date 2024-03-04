package proxies.interceptors

import io.ktor.http.*
import rtmp.amf0.Amf0Node
import simpleJson.JsonNode

sealed interface Call {
    sealed interface RmsCall : Call {
        val data: JsonNode

        data class RmsRequest(override val data: JsonNode) : RmsCall
        data class RmsResponse(override val data: JsonNode) : RmsCall
    }

    sealed interface XmppCall : Call {
        val data: String

        data class XmppRequest(override val data: String) : XmppCall
        data class XmppResponse(override val data: String) : XmppCall
    }

    sealed interface ConfigCall : Call {
        val data: JsonNode
        val url: String
        val headers: Headers

        data class ConfigResponse(
            override val data: JsonNode,
            override val url: String,
            override val headers: Headers,
        ) : ConfigCall
    }

    sealed interface RtmpCall : Call {
        val data: List<Amf0Node>

        data class RtmpRequest(override val data: List<Amf0Node>) : RtmpCall
        data class RtmpResponse(override val data: List<Amf0Node>) : RtmpCall
    }
}