package proxies.interceptors

import okhttp3.Headers
import org.w3c.dom.Document
import rtmp.amf0.Amf0Node
import simpleJson.JsonNode

sealed interface Call {
    sealed interface XmppCall : Call {
        val data: Document

        data class XmppRequest(override val data: Document) : XmppCall
        data class XmppResponse(override val data: Document) : XmppCall
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