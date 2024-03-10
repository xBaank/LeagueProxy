package scripting

import shared.Call
import java.io.File
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.valueOrNull
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
    }

    val result = BasicJvmScriptingHost().eval(scriptFile, compilationConfiguration, null)
    val function = (result.valueOrNull()?.returnValue as? ResultValue.Value)?.value as (Call) -> Call
    return function
}