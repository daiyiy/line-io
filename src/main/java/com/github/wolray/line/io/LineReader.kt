package com.github.wolray.line.io

import com.github.wolray.seq.IsReader
import com.github.wolray.seq.IsReader.InputSource
import com.github.wolray.seq.Seq
import com.github.wolray.seq.WithCe
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import kotlin.streams.asStream

/**
 * @author wolray
 */
abstract class LineReader<S, V, T> protected constructor(val converter: ValuesConverter<V, *, T>) {
    protected var limit = 0

    fun read(source: S): Session = Session(source)

    protected abstract fun splitHeader(v: V): List<String>
    protected abstract fun toIterator(source: S): Iterator<V>
    protected abstract fun toSeq(source: S): Seq<V>

    open class Csv<T> internal constructor(val sep: String, valuesConverter: ValuesConverter.Csv<T>) :
        LineReader<InputSource, List<String>, T>(valuesConverter),
        IsReader<LineReader<InputSource, List<String>, T>.Session> {

        override fun splitHeader(v: List<String>): List<String> = v

        override fun toIterator(source: InputSource): Iterator<List<String>> {
            return BufferedReader(InputStreamReader(source.call()))
                .lineSequence()
                .map { it.split(sep, limit = limit) }
                .iterator()
        }

        override fun toSeq(source: InputSource): Seq<List<String>> = Seq {
            BufferedReader(InputStreamReader(source.call())).use { reader ->
                while (true) {
                    val s = reader.readLine()
                    if (s != null) it.accept(s.split(sep, limit = limit)) else break
                }
            }
        }
    }

    class Excel<T> internal constructor(
        private val sheetIndex: Int,
        converter: ValuesConverter.Excel<T>
    ) : LineReader<InputSource, Row, T>(converter),
        IsReader<LineReader<InputSource, Row, T>.Session> {

        override fun splitHeader(v: Row): List<String> = v.map { it.stringCellValue }

        override fun toIterator(source: InputSource): Iterator<Row> {
            return XSSFWorkbook(source.call()).getSheetAt(sheetIndex).iterator()
        }

        override fun toSeq(source: InputSource): Seq<Row> {
            return Seq.of(XSSFWorkbook(source.call()).getSheetAt(sheetIndex))
        }
    }

    inner class Session(private val source: S) : Chainable<Session> {
        private var errorType: Class<out Exception>? = null
        private var skip: Int = 0
        private var slots: IntArray? = null
        private var cols: Array<String>? = null
        override val self get() = this

        fun ignoreError(type: Class<out Exception>) = apply { errorType = type }
        fun skipLines(n: Int) = apply { skip = n }
        @Deprecated("Bad name, use columns instead", ReplaceWith("columns"))
        fun csvHeader(vararg col: String) = columns(*col)
        fun columns(vararg col: String) = apply { cols = arrayOf(*col) }
        fun columns(vararg index: Int) = apply { slots = index }
        fun columnsBefore(index: Int) = columnsRange(0, index)
        fun columnsRange(start: Int, before: Int) = apply { slots = rangeOf(start, before) }

        fun excelColumns(excelCols: String) = apply {
            slots = if (excelCols.isEmpty()) intArrayOf()
            else excelCols.split(",").map(::excelIndex).toIntArray()
        }

        private fun excelIndex(col: String): Int {
            return col.trim().fold(0) { acc, c -> acc * 26 + (c - A + 1) } - 1
        }

        private fun preprocess(v: V): Boolean {
            var res = false
            cols?.apply {
                if (isNotEmpty()) {
                    val split = splitHeader(v)
                    slots = toSlots(split)
                    res = true
                }
            }
            reorder()
            return res
        }

        private fun preprocess(iterator: Iterator<V>) {
            cols?.apply {
                if (isNotEmpty()) {
                    val split = splitHeader(iterator.next())
                    slots = toSlots(split)
                }
            }
            reorder()
        }

        private fun reorder() {
            limit = limit.coerceAtLeast(converter.typeValues.size + 1)
            slots?.apply {
                if (isNotEmpty()) {
                    converter.resetOrder(this)
                    limit = limit.coerceAtLeast(max() + 2)
                }
            }
        }

        private fun tempIterator(): Iterator<V> = WithCe.call(errorType) {
            toIterator(source).apply {
                if (skip > 0) {
                    repeat(skip) { next() }
                }
                preprocess(this)
            }
        } ?: Collections.emptyIterator()

        fun sequence(): Sequence<T> = Sequence(::iterator)

        @Deprecated("Use better toSeq", replaceWith = ReplaceWith("toSeq"))
        fun stream(): DataStream<T> = DataStream.of {
            tempIterator().asSequence().map(converter).asStream()
        }

        fun iterator() = object : Iterator<T> {
            val iterator = tempIterator()
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): T = converter(iterator.next())
        }

        fun toSeq(): Seq<T> = Seq {
            var i = skip
            WithCe.call(errorType) { toSeq(source) }?.supply { t ->
                if (i < 0 || i-- == 0 && !preprocess(t)) {
                    it.accept(converter(t))
                }
            }
        }
    }

    companion object {
        private const val A = 'A'

        internal fun Array<String>.toSlots(split: List<String>): IntArray = map {
            split.indexOf(it).apply {
                if (this < 0) throw NoSuchElementException("$it not in $split")
            }
        }.toIntArray()

        @JvmStatic
        fun <T> byCsv(sep: String, type: Class<T>): Csv<T> {
            return Csv(sep, ValuesConverter.Csv(TypeValues(type)))
        }

        @JvmStatic
        fun <T> byCsv(sep: String, dataMapper: DataMapper<T>): Csv<T> {
            return Csv(sep, ValuesConverter.Csv(dataMapper.typeValues))
        }

        @JvmStatic
        fun <T> byExcel(type: Class<T>) = byExcel(0, type)

        @JvmStatic
        fun <T> byExcel(sheetIndex: Int, type: Class<T>): Excel<T> {
            return Excel(sheetIndex, ValuesConverter.Excel(TypeValues(type)))
        }

        private fun rangeOf(start: Int, before: Int): IntArray {
            return (start until before).toList().toIntArray()
        }
    }
}
