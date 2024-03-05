package proxies.interceptors

import io.ktor.http.*
import rtmp.amf0.Amf0Node
import simpleJson.JsonNode

sealed interface Call {
    sealed interface HttpCall : Call {
        var data: JsonNode?
        val url: String
        var headers: Headers
        val method: HttpMethod
        val statusCode: HttpStatusCode?
    }

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

    sealed interface ConfigCall : HttpCall {

        data class ConfigRequest(
            override var data: JsonNode?,
            override val url: String,
            override var headers: Headers,
            override val method: HttpMethod,
            override val statusCode: HttpStatusCode?,
        ) : ConfigCall

        data class ConfigResponse(
            override var data: JsonNode?,
            override val url: String,
            override var headers: Headers,
            override val method: HttpMethod,
            override val statusCode: HttpStatusCode,
        ) : ConfigCall
    }

    sealed interface RedEdgeCall : HttpCall {
        data class RedEdgeResponse(
            val port: Int,
            override var data: JsonNode?,
            override val url: String,
            override var headers: Headers,
            override val method: HttpMethod,
            override val statusCode: HttpStatusCode?,
        ) : RedEdgeCall

        data class RedEdgeRequest(
            override var data: JsonNode?,
            override val url: String,
            override var headers: Headers,
            override val method: HttpMethod,
            override val statusCode: HttpStatusCode?,
        ) : RedEdgeCall
    }

    sealed interface RiotAuthCall : HttpCall {
        data class RiotAuthResponse(
            val port: Int,
            override var data: JsonNode?,
            override val url: String,
            override var headers: Headers,
            override val method: HttpMethod,
            override val statusCode: HttpStatusCode?,
        ) : RiotAuthCall

        data class RiotAuthRequest(
            override var data: JsonNode?,
            override val url: String,
            override var headers: Headers,
            override val method: HttpMethod,
            override val statusCode: HttpStatusCode?,
        ) : RiotAuthCall
    }

    sealed interface RtmpCall : Call {
        val data: List<Amf0Node>

        data class RtmpRequest(override val data: List<Amf0Node>) : RtmpCall
        data class RtmpResponse(override val data: List<Amf0Node>) : RtmpCall
    }
}