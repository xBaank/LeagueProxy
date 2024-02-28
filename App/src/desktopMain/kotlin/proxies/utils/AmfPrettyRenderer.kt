package proxies.utils

import rtmp.amf0.*


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

    fun renderAmf0Amf3(node: Amf0Amf3) {
        builder.append(node.nodes.toString())
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