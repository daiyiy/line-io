package com.github.wolray.line.io

import java.io.InputStream

/**
 * @author wolray
 */
class CsvReader<T> internal constructor(
    private val converter: ValuesConverter.Text<T>,
    private val sep: String
) : LineReader.Text<T>(converter.toParser(sep)) {

    fun read(file: String) = Session(toInputStream(file)!!)

    override fun read(source: InputStream) = Session(source)

    override fun reorder(slots: IntArray) = converter.resetOrder(slots)

    private fun setHeader(s: String, header: Array<String>) {
        val list = s.split(sep.toRegex())
        val slots = header
            .map { list.indexOf(it) }
            .toIntArray()
        slots.zip(header).forEach {
            if (it.first < 0) {
                throw NoSuchElementException(it.second)
            }
        }
        reorder(slots)
    }

    inner class Session internal constructor(input: InputStream) :
        LineReader<InputStream, String, T>.Session(input) {
        private var cols: Array<String>? = null

        override fun skipLines(n: Int) = apply { super.skipLines(n) }

        fun csvHeader(vararg useCols: String) = apply { cols = arrayOf(*useCols) }

        override fun preprocess(iterator: Iterator<String>) {
            cols.testIf { isNotEmpty() }
                .call { setHeader(iterator.next(), this) }
                .orElse { super.preprocess(iterator) }
        }
    }

    companion object {
        @JvmStatic
        fun <T> of(sep: String, type: Class<T>) = byCsv(sep, type)
    }
}
