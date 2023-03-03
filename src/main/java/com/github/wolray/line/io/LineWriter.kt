package com.github.wolray.line.io

import com.alibaba.fastjson.JSON
import com.github.wolray.line.io.CommonWriter.Companion.commonWriter
import com.github.wolray.seq.Seq
import java.io.BufferedWriter
import java.io.FileWriter
import java.util.*
import java.util.function.Function

/**
 * @author wolray
 */
open class LineWriter<T>(private val formatter: Function<T, String>) {

    open fun write(seq: Seq<T>) = TextSession(seq)

    open inner class TextSession(val seq: Seq<T>) : Chainable<TextSession> {
        private val headers: MutableList<String> = LinkedList()
        override val self get() = this

        fun addHeader(header: String) = apply { headers.add(header) }
        open fun markUtf8() = this
        open fun autoHeader() = this
        open fun columns(vararg names: String) = this
        protected open fun preprocess(writer: CommonWriter<*>) {}

        @JvmOverloads
        fun toFile(file: String, append: Boolean = false) {
            BufferedWriter(FileWriter(file, append)).use { bw ->
                bw.commonWriter().run {
                    if (!append) {
                        preprocess(this)
                        headers.forEach(::writeLine)
                    }
                    seq.reduce(toReducer(formatter))
                }
            }
        }

        fun toContent(): String = StringBuilder().commonWriter().run {
            preprocess(this)
            headers.forEach(::writeLine)
            seq.reduce(toReducer(formatter)).toString()
        }
    }

    companion object {
        @JvmStatic
        fun <T> byJson() = LineWriter<T> { JSON.toJSONString(it) }
    }
}
