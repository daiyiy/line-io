package com.github.wolray.line.io

import com.alibaba.fastjson.JSON
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.IOException
import java.io.UncheckedIOException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.Function

/**
 * @author wolray
 */
open class LineWriter<T>(private val formatter: Function<T, String>) {

    open fun write(file: String) = Session(file)

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
            asyncWith(iterable::forEach)
        }

        fun asyncWith(seq: Consumer<Consumer<T>>) {
            CompletableFuture.runAsync { with(seq) }
        }

        fun with(iterable: Iterable<T>) {
            with(iterable::forEach)
        }

        fun with(seq: Consumer<Consumer<T>>) {
            try {
                BufferedWriter(FileWriter(file, append)).use { bw ->
                    if (!append) {
                        preprocess(bw)
                        headers.forEach { bw.writeLine(it) }
                    }
                    seq.accept { bw.writeLine(formatter.apply(it)) }
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
        fun <T> byCsv(sep: String, type: Class<T>): CsvWriter<T> {
            return CsvWriter(ValuesJoiner(TypeValues(type)), sep)
        }

        fun BufferedWriter.writeLine(s: String) {
            write(s)
            newLine()
        }
    }
}
