package shared

import io.ktor.http.*
import rtmp.amf0.Amf0Node
import shared.proxies.HttpProxy
import shared.proxies.RmsProxy
import shared.proxies.XmppProxy
import simpleJson.JsonNode

sealed interface Call {
    sealed interface HttpCall : Call {
        val body: Body
        val url: String
        var headers: Headers
        val method: HttpMethod
        var statusCode: HttpStatusCode?
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
            override val body: Body,
            override val url: String,
            override var headers: Headers,
            override val method: HttpMethod,
            override var statusCode: HttpStatusCode?,
        ) : ConfigCall

        data class ConfigResponse(
            override val body: Body,
            override val url: String,
            override var headers: Headers,
            override val method: HttpMethod,
            override var statusCode: HttpStatusCode?,
            val xmppProxies: Map<String, XmppProxy>,
            val rmsProxies: Set<RmsProxy>,
            val redEdgeProxies: Set<HttpProxy>,
            val riotAuthProxy: HttpProxy,
            val riotAuthenticateProxy: HttpProxy,
            val rioEntitlementAuthProxy: HttpProxy,
            val riotAffinityServer: HttpProxy,
        ) : ConfigCall
    }

    sealed interface RedEdgeCall : HttpCall {
        data class RedEdgeResponse(
            override val body: Body,
            override val url: String,
            override var headers: Headers,
            override val method: HttpMethod,
            override var statusCode: HttpStatusCode?,
        ) : RedEdgeCall

        data class RedEdgeRequest(
            override val body: Body,
            override val url: String,
            override var headers: Headers,
            override val method: HttpMethod,
            override var statusCode: HttpStatusCode?,
        ) : RedEdgeCall
    }

    sealed interface RiotAuthCall : HttpCall {
        data class RiotAuthResponse(
            val port: Int,
            override val body: Body,
            override val url: String,
            override var headers: Headers,
            override val method: HttpMethod,
            override var statusCode: HttpStatusCode?,
        ) : RiotAuthCall

        data class RiotAuthRequest(
            override val body: Body,
            override val url: String,
            override var headers: Headers,
            override val method: HttpMethod,
            override var statusCode: HttpStatusCode?,
        ) : RiotAuthCall
    }

    sealed interface RtmpCall : Call {
        val data: List<Amf0Node>

        data class RtmpRequest(override val data: List<Amf0Node>) : RtmpCall
        data class RtmpResponse(override val data: List<Amf0Node>) : RtmpCall
    }
}