package scripting

import exceptions.ScriptException
import io.github.oshai.kotlinlogging.KotlinLogging
import shared.Call
import java.io.File
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

private val logger = KotlinLogging.logger {}

fun eval(scriptFile: File) = eval(scriptFile.toScriptSource())
fun eval(value: String) = eval(value.toScriptSource())

fun eval(scriptFile: SourceCode): (Call) -> Call {
    logger.info { "loading script ${scriptFile.name}" }

    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScript> {
        jvm {
            dependenciesFromCurrentContext(
                wholeClasspath = true
            )
        }
        compilerOptions.append("-Xadd-modules=ALL-MODULE-PATH")
    }

    val result = BasicJvmScriptingHost().eval(scriptFile, compilationConfiguration, null)
    val value = runCatching { result.valueOrThrow() }.getOrElse { throw ScriptException(it) }

    logger.info { "loaded script ${scriptFile.name} with value ${value.returnValue}" }

    return when (val returnValue = value.returnValue) {
        is ResultValue.Error -> throw ScriptException(returnValue.error)
        ResultValue.NotEvaluated -> throw ScriptException(message = "Not evaluated")
        is ResultValue.Unit -> throw ScriptException(message = "Script cant return Unit")
        is ResultValue.Value -> (returnValue).value as (Call) -> Call
    }
}