package com.github.wolray.line.io

import com.github.wolray.line.io.EmptyScope.ifNotEmpty
import com.github.wolray.seq.Seq

/**
 * @author wolray
 */
class CsvWriter<T> internal constructor(
    private val joiner: ValuesJoiner<T>,
    private val sep: String
) : LineWriter<T>(joiner.toFormatter(sep)) {

    override fun write(seq: Seq<T>) = object : LineWriter<T>.TextSession(seq) {
        private var utf8 = false
        override fun markUtf8() = apply { utf8 = true }
        override fun autoHeader() = apply { addHeader(joiner.joinFields(sep)) }

        override fun columns(vararg names: String) = apply {
            names.ifNotEmpty { addHeader(joinToString(sep)) }
        }

        override fun preprocess(writer: CommonWriter<*>) {
            if (utf8) writer.write(utf8Marker)
        }
    }

    companion object {
        const val utf8Marker = '\ufeff'

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
