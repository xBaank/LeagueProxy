package proxies.interceptors

import kotlinx.coroutines.flow.MutableSharedFlow
import proxies.interceptors.Call.RtmpCall
import proxies.interceptors.Call.RtmpCall.RtmpRequest
import proxies.interceptors.Call.RtmpCall.RtmpResponse
import proxies.utils.base64Ungzip
import proxies.utils.gzipBase64
import rtmp.amf0.*
import simpleJson.deserialized
import simpleJson.get
import simpleJson.serialized
import simpleJson.set

class RTMPProxyInterceptor : IProxyInterceptor<List<Amf0Node>, RtmpCall> {
    val calls: MutableSharedFlow<RtmpCall> = MutableSharedFlow()

    override suspend fun onRequest(value: List<Amf0Node>): RtmpCall {
        val request = RtmpRequest(value)
        calls.emit(request)
        return request
    }

    override suspend fun onResponse(value: List<Amf0Node>): RtmpCall {
        val response = RtmpResponse(value)

        rtmpTweak(value)

        calls.emit(response)

        return response
    }

    private fun rtmpTweak(value: List<Amf0Node>) {
        try {
            val body = value[3].toAmf0TypedObject()
                ?.value
                ?.get("body")
                .toAmf0TypedObject()

            val config = body?.get("configs").toAmf0String()?.value?.base64Ungzip()?.deserialized()?.getOrNull()
            if (config == null) TODO()

            config["ChampionSelect"]["UseOptimizedBotChampionSelectProcessor"] = true
            config["ChampionSelect"]["UseOptimizedChampSelectProcessor"] = true
            config["ChampionSelect"]["UseOptimizedSpellSelectProcessor"] = true
            config["ChampionSelect"]["AllChampsAvailableInAram"] = true

            config["LcuChampionSelect"]["PositionAssignmentAnimationEnabled"] = false

            config["LeaverBuster"]["IsLockoutModalEnabled"] = false
            config["LeaverBuster"]["IsLbsEnabled"] = false

            config["LcuLobby"]["QueueEligibilityGateKeeperEnabled"] = false

            config["CustomGame"]["BotsAvailableInAram"] = true

            config["LcuTutorial"]["CarouselChampIds"] = "21,122,103,11,901"
            config["LcuTutorial"]["IntroABTestPercentage"] = 0

            config["LootConfig"]["NewPlayerChestEnabled"] = true
            config["LootConfig"]["EventChestsEnabled"] = true
            config["LootConfig"]["ChestBundleDiscount5"] = 100
            config["LootConfig"]["ChestBundleDiscount4"] = 100

            config["LcuSocial"]["ClearChatHistoryEnabled"] = false

            config["LcuLoyalty"]["LeagueUnlockedEnabled"] = true
            config["LcuLoyalty"]["LolcafeEnabled"] = true

            config["LcuHome"]["RequireItemLoaded"] = false


            body?.set("configs", Amf0String(config.serialized().gzipBase64()))


        } catch (_: Throwable) {

        }

        try {
            val node = value[3].toAmf0TypedObject()?.value?.get("body").toAmf0TypedObject()?.get("accountSummary")
                .toAmf0TypedObject()
            node?.set("admin", Amf0Boolean(true))

        } catch (_: Throwable) {

        }
    }
}