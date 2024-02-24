package proxies

sealed interface RtmpInterceptType {
    data object Request : RtmpInterceptType
    data object Response : RtmpInterceptType
}