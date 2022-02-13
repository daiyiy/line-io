package com.github.wolray.line.io

import java.io.BufferedWriter
import java.io.IOException

/**
 * @author wolray
 */
class CsvWriter<T> internal constructor(
    private val joiner: ValuesJoiner<T>,
    private val sep: String
) : LineWriter<T>(joiner.toFormatter(sep)) {

    override fun write(file: String) = Session(file)

    inner class Session internal constructor(file: String) :
        LineWriter<T>.Session(file) {
        private var utf8 = false

        fun markUtf8(): Session {
            utf8 = true
            return this
        }

        fun autoHeader(): Session {
            addHeader(joiner.join(sep) { it.field.name })
            return this
        }

        fun columnNames(vararg names: String): Session {
            if (names.isNotEmpty()) {
                addHeader(names.joinToString(sep))
            }
            return this
        }

        @Throws(IOException::class)
        override fun preprocess(bw: BufferedWriter) {
            if (utf8 && file.endsWith(".csv")) {
                bw.write('\ufeff'.code)
            }
        }
    }
}