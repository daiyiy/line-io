package com.github.wolray.line.io

import com.alibaba.fastjson.JSON
import com.github.wolray.line.io.CommonWriter.Companion.commonWriter
import com.github.wolray.seq.Seq
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.IOException
import java.io.UncheckedIOException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Function

/**
 * @author wolray
 */
open class LineWriter<T>(private val formatter: Function<T, String>) {

    open fun write(seq: Seq<T>) = TextSession(seq)

    @Deprecated("Use write(seq) instead", ReplaceWith("write(seq)"))
    open fun write(file: String) = Session(file)

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
            preprocess("", this)
            headers.forEach(::writeLine)
            seq.reduce(toReducer(formatter)).toString()
        }
    }

    open inner class Session(protected val file: String) : Chainable<Session> {
        private val headers: MutableList<String> = LinkedList()
        private var append = false
        override val self get() = this

        fun addHeader(header: String) = apply { headers.add(header) }
        fun appendToFile() = apply { append = true }

        open fun markUtf8() = this
        open fun autoHeader() = this
        open fun columnNames(vararg names: String) = this

        protected open fun preprocess(bw: BufferedWriter) {}

        fun asyncWith(iterable: Iterable<T>) {
            CompletableFuture.runAsync { with(iterable) }
        }

        fun with(iterable: Iterable<T>) {
            try {
                BufferedWriter(FileWriter(file, append)).use { bw ->
                    if (!append) {
                        preprocess(bw)
                        headers.forEach { bw.writeLine(it) }
                    }
                    iterable.forEach { bw.writeLine(formatter.apply(it)) }
                }
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }
        }
    }

    companion object {
        @JvmStatic
        fun <T> byJson() = LineWriter<T> { JSON.toJSONString(it) }

        @JvmStatic
        @Deprecated("Unnecessary, use CsvWriter.of", ReplaceWith("CsvWriter.of"))
        fun <T> byCsv(sep: String, type: Class<T>): CsvWriter<T> {
            return CsvWriter.of(sep, type)
        }

        private fun BufferedWriter.writeLine(s: String) {
            write(s)
            newLine()
        }
    }
}
