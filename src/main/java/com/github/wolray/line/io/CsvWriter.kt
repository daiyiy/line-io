package com.github.wolray.line.io

import com.github.wolray.line.io.EmptyScope.ifNotEmpty
import com.github.wolray.seq.Seq
import java.io.BufferedWriter

/**
 * @author wolray
 */
class CsvWriter<T> internal constructor(
    private val joiner: ValuesJoiner<T>,
    private val sep: String
) : LineWriter<T>(joiner.toFormatter(sep)) {

    override fun write(seq: Seq<T>) = CsvSession(seq)

    @Deprecated("Use write(seq) instead", ReplaceWith("write(seq)"))
    override fun write(file: String) = Session(file)

    inner class CsvSession internal constructor(seq: Seq<T>) : LineWriter<T>.TextSession(seq) {
        private var utf8 = false

        override fun markUtf8() = apply { utf8 = true }
        override fun autoHeader() = apply { addHeader(joiner.joinFields(sep)) }

        override fun columns(vararg names: String) = apply {
            names.ifNotEmpty { addHeader(joinToString(sep)) }
        }

        override fun preprocess(file: String, writer: CommonWriter<*>) {
            if (utf8 && file.endsWith(".csv")) {
                writer.write('\ufeff')
            }
        }
    }
    
    inner class Session internal constructor(file: String) : LineWriter<T>.Session(file) {
        private var utf8 = false

        override fun markUtf8() = apply { utf8 = true }
        override fun autoHeader() = apply { addHeader(joiner.joinFields(sep)) }

        override fun columnNames(vararg names: String) = apply {
            names.ifNotEmpty { addHeader(joinToString(sep)) }
        }

        override fun preprocess(bw: BufferedWriter) {
            if (utf8 && file.endsWith(".csv")) {
                bw.write('\ufeff'.code)
            }
        }
    }

    companion object {
        @JvmStatic
        fun <T> of(sep: String, type: Class<T>): CsvWriter<T> {
            return CsvWriter(ValuesJoiner(TypeValues(type)), sep)
        }

        @JvmStatic
        fun <T> of(sep: String, mapper: DataMapper<T>): CsvWriter<T> {
            return CsvWriter(ValuesJoiner(mapper.typeValues), sep)
        }
    }
}
