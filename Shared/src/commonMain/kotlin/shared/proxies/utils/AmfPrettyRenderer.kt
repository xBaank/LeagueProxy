package shared.proxies.utils

import rtmp.amf0.*
import rtmp.amf3.*


class Amf0PrettyBuilder(private var indentLevel: Int = 0) {
    private val builder: StringBuilder = StringBuilder()

    fun build() = builder.toString()

    fun write(node: Amf0Node): Amf0PrettyBuilder {
        when (node) {
            is Amf0Amf3 -> renderAmf0Amf3(node)
            is Amf0Boolean -> renderAmf0Boolean(node)
            is Amf0Date -> renderAmf0Date(node)
            is Amf0ECMAArray -> renderAmf0EcmaArray(node)
            Amf0Null -> renderAmf0Null()
            is Amf0Number -> renderAmf0Number(node)
            is Amf0Object -> renderAmf0Object(node)
            is Amf0Reference -> renderAmf0Reference(node)
            is Amf0StrictArray -> renderAmf0StrictArray(node)
            is Amf0String -> renderAmf0String(node)
            is Amf0TypedObject -> renderAmf0TypedObject(node)
            Amf0Undefined -> renderAmf0Undefined()
        }

        return this
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun write(node: Amf3Node) {
        when (node) {
            is Amf3Array -> renderAmf3Array(node)
            is Amf3ByteArray -> builder.append(node.value.toHexString())
            is Amf3Date -> renderAmf0Date(Amf0Date(node.value, 0))
            Amf3Dictionary -> TODO()
            is Amf3Double -> renderAmf0Number(Amf0Number(node.value))
            Amf3False -> builder.append(false)
            is Amf3Integer -> renderAmf0Number(Amf0Number(node.value.toDouble()))
            Amf3Null -> builder.append(null.toString())
            is Amf3Object -> renderAmf3Object(node)
            is Amf3String -> renderAmf0String(Amf0String(node.value))
            Amf3True -> builder.append(true)
            Amf3Undefined -> builder.append("undefined")
            is Amf3VectorDouble -> TODO()
            is Amf3VectorInt -> TODO()
            is Amf3VectorObject -> TODO()
            is Amf3VectorUint -> TODO()
            is Amf3XMLDocument -> TODO()
        }
    }

    fun renderAmf3Object(node: Amf3Object) {
        builder.append("{")
        if (node.value.isNotEmpty()) {
            builder.appendLine()
            indentLevel++
            node.value.entries.forEachIndexed { index, (key, value) ->
                if (index != 0) {
                    builder.append(",")
                    builder.appendLine()
                }
                writeIndent()
                builder.append(key)
                builder.append(": ")
                write(value)
            }
            builder.appendLine()
            indentLevel--
            writeIndent()
        }
        builder.append("}")
    }

    fun renderAmf3Array(node: Amf3Array) {
        builder.append("[")
        if (node.value.isNotEmpty()) {
            builder.appendLine()
            indentLevel++
            node.value.forEachIndexed { index, node ->
                if (index != 0) {
                    builder.append(",")
                    builder.appendLine()
                }
                writeIndent()
                write(node)
            }
            builder.appendLine()
            indentLevel--
            writeIndent()
        }
        builder.append("]")
    }

    fun renderAmf0Amf3(node: Amf0Amf3) {
        renderAmf3Array(Amf3Array(node.nodes.toMutableList()))
    }

    fun renderAmf0Boolean(node: Amf0Boolean) {
        builder.append(node.value)
    }

    fun renderAmf0Date(node: Amf0Date) {
        builder.append(node.value)
    }

    fun renderAmf0Null() {
        builder.append(null.toString())
    }

    fun renderAmf0Undefined() {
        builder.append("undefined")
    }

    fun renderAmf0Reference(node: Amf0Reference) {
        builder.append(node.value)
    }

    fun renderAmf0Number(node: Amf0Number) {
        builder.append(node.value)
    }

    fun renderAmf0String(node: Amf0String) {
        builder.append("\"${node.value}\"")
    }

    fun renderAmf0StrictArray(node: Amf0StrictArray) {
        builder.append("[")
        if (node.value.isNotEmpty()) {
            builder.appendLine()
            indentLevel++
            node.value.forEachIndexed { index, node ->
                if (index != 0) {
                    builder.append(",")
                    builder.appendLine()
                }
                writeIndent()
                write(node)
            }
            builder.appendLine()
            indentLevel--
            writeIndent()
        }
        builder.append("]")

    }

    fun renderAmf0Object(node: Amf0Object) {
        builder.append("{")
        if (node.value.isNotEmpty()) {
            builder.appendLine()
            indentLevel++
            node.value.entries.forEachIndexed { index, (key, value) ->
                if (index != 0) {
                    builder.append(",")
                    builder.appendLine()
                }
                writeIndent()
                builder.append(key)
                builder.append(": ")
                write(value)
            }
            builder.appendLine()
            indentLevel--
            writeIndent()
        }
        builder.append("}")
    }

    fun renderAmf0EcmaArray(node: Amf0ECMAArray) {
        builder.append("[")
        if (node.value.isNotEmpty()) {
            builder.appendLine()
            indentLevel++
            node.value.entries.forEachIndexed { index, (key, value) ->
                if (index != 0) {
                    builder.append(",")
                    builder.appendLine()
                }
                writeIndent()
                builder.append(key)
                builder.append(": ")
                write(value)
            }
            builder.appendLine()
            indentLevel--
            writeIndent()
        }
        builder.append("]")
    }

    fun renderAmf0TypedObject(node: Amf0TypedObject) {
        builder.append("{")
        if (node.value.isNotEmpty()) {
            builder.appendLine()
            indentLevel++
            node.value.entries.forEachIndexed { index, (key, value) ->
                if (index != 0) {
                    builder.append(",")
                    builder.appendLine()
                }
                writeIndent()
                builder.append(key)
                builder.append(": ")
                write(value)
            }
            builder.appendLine()
            indentLevel--
            writeIndent()
        }
        builder.append("}")
    }

    private fun writeIndent() {
        repeat(indentLevel) {
            builder.append("  ")
        }
    }
}