package shared.proxies.utils

import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process

suspend fun isRiotClientRunning(): Boolean {
    val process = process(
        "WMIC",
        "PROCESS",
        "WHERE",
        "name='RiotClientServices.exe'",
        "GET",
        "commandline",
        stdout = Redirect.CAPTURE
    )
    return process.output.map(String::trim).contains("CommandLine")
}

suspend fun killRiotClient() {
    process("taskkill", "/F", "/IM", "RiotClientServices.exe")
}