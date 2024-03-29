package com.github.wolray.line.io

import com.github.wolray.seq.Reducer
import java.io.BufferedWriter
import java.util.function.Function

/**
 * @author wolray
 */
abstract class CommonWriter<W>(val backer: W) {
    abstract fun write(s: String): Any
    abstract fun write(c: Char): Any
    
    fun writeLine(s: String) {
        write(s)
        write('\n')
    }

    fun <T> toReducer(formatter: Function<T, String>): Reducer<T, W> = Reducer.of(
        { backer },
        { _, t -> writeLine(formatter.apply(t)) }
    )

    companion object {
        @JvmStatic
        fun BufferedWriter.commonWriter() = object : CommonWriter<BufferedWriter>(this@commonWriter) {
            override fun write(s: String) = backer.write(s)
            override fun write(c: Char) = backer.write(c.code)
        }

        @JvmStatic
        fun StringBuilder.commonWriter() = object : CommonWriter<StringBuilder>(this@commonWriter) {
            override fun write(s: String) = append(s)
            override fun write(c: Char) = append(c)
        }
    }
}
