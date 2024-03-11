package scripting

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

fun eval(scriptFile: File) = eval(scriptFile.toScriptSource())
fun eval(value: String) = eval(value.toScriptSource())

fun eval(scriptFile: SourceCode): (Call) -> Call {
    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScript> {
        jvm {
            dependenciesFromCurrentContext(
                wholeClasspath = true
            )
        }
        compilerOptions.append("-Xadd-modules=ALL-MODULE-PATH")
    }

    val result = BasicJvmScriptingHost().eval(scriptFile, compilationConfiguration, null)
    val value = result.valueOrThrow()
    return when (val returnValue = value.returnValue) {
        is ResultValue.Error -> throw returnValue.error
        ResultValue.NotEvaluated -> throw RuntimeException("Not evaluated")
        is ResultValue.Unit -> throw RuntimeException("Script cant return Unit")
        is ResultValue.Value -> (returnValue).value as (Call) -> Call
    }
}