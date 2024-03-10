package scripting

import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.classpathFromClass
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

inline fun <reified T : Any> eval(scriptFile: File, data: T) = eval(scriptFile.toScriptSource(), data)
inline fun <reified T : Any> eval(value: String, data: T) = eval(value.toScriptSource(), data)

inline fun <reified T : Any> eval(scriptFile: SourceCode, data: T): ResultWithDiagnostics<EvaluationResult> {
    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<SimpleScript> {
        updateClasspath(classpathFromClass<T>())
        jvm {
            dependenciesFromCurrentContext(
                wholeClasspath = true
            )
        }
    }

    val result = BasicJvmScriptingHost().eval(scriptFile, compilationConfiguration, null)
    val function = (result.valueOrNull()?.returnValue as? ResultValue.Value)?.value as (T) -> String
    println(function(data))
    return result
}