package com.github.wolray.line.io

import com.github.wolray.line.io.EmptyScope.ifNotEmpty
import com.github.wolray.line.io.IteratorScope.map
import com.github.wolray.line.io.SeqScope.seq
import com.github.wolray.line.io.TypeScope.ignorableToCall
import com.github.wolray.seq.IsReader
import com.github.wolray.seq.IsReader.InputSource
import com.github.wolray.seq.Seq
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.util.*
import kotlin.streams.asStream

/**
 * @author wolray
 */
abstract class LineReader<S, V, T> protected constructor(val converter: ValuesConverter<V, *, T>) {
    protected var skip = 0
    protected var limit = 0

    fun read(source: S): Session = Session(source)

    protected open fun getHeader(source: S): List<String>? = null
    protected abstract fun splitHeader(v: V): List<String>
    protected abstract fun toIterator(source: S): Iterator<V>
    protected abstract fun toSeq(source: S): Seq<V>

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
            return XSSFWorkbook(source.call()).getSheetAt(sheetIndex).seq()
        }
    }

    inner class Session(private val source: S) : Chainable<Session> {
        private var errorType: Class<out Exception>? = null
        private var slots: IntArray? = null
        private var cols: Array<String>? = null
        override val self get() = this

        fun ignoreError(type: Class<out Exception>) = apply { errorType = type }
        fun skipLines(n: Int) = apply { skip = n }
        @Deprecated("Bad name, use columns instead", ReplaceWith("this.columns()"))
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

        private fun forHeader(iterator: Iterator<V>) {
            cols?.ifNotEmpty {
                val split = getHeader(source) ?: splitHeader(iterator.next())
                slots = toSlots(split)
            }
            reorder()
        }

        private fun forHeader(v: V): Boolean {
            var res = false
            cols?.ifNotEmpty {
                val header = getHeader(source) ?: splitHeader(v).also { res = true }
                slots = toSlots(header)
            }
            reorder()
            return res
        }

        private fun reorder() {
            limit = limit.coerceAtLeast(converter.typeValues.size + 1)
            slots?.ifNotEmpty {
                converter.resetOrder(this)
                limit = limit.coerceAtLeast((maxOrNull() ?: 0) + 2)
            }
        }

        fun sequence(): Sequence<T> = Sequence(::toIterator)

        @Deprecated("Use better toSeq", replaceWith = ReplaceWith("toSeq"))
        fun stream(): DataStream<T> = DataStream.of {
            toIterator().asSequence().asStream()
        }

        fun toIterator(): Iterator<T> = errorType.ignorableToCall {
            toIterator(source).apply {
                if (skip > 0) {
                    repeat(skip) { next() }
                }
                forHeader(this)
            }
        }?.map(converter) ?: Collections.emptyIterator()

        fun toSeq(): Seq<T> = Seq {
            errorType.ignorableToCall {
                var i = skip
                toSeq(source).supply { v ->
                    if (i < 0 || i-- == 0 && !forHeader(v)) {
                        it.accept(converter(v))
                    }
                }
            }
        }
    }

    companion object {
        private const val A = 'A'

        fun Array<String>.toSlots(header: List<String>): IntArray = map {
            header.indexOf(it).apply {
                if (this < 0) throw NoSuchElementException("$it not in $header")
            }
        }.toIntArray()

        @JvmStatic
        @Deprecated("Unnecessary, use CsvReader.of", ReplaceWith("CsvReader.of"))
        fun <T> byCsv(sep: String, type: Class<T>): CsvReader<T> = CsvReader.of(sep, type)

        @JvmStatic
        fun <T> byExcel(type: Class<T>): Excel<T> = byExcel(0, type)

        @JvmStatic
        fun <T> byExcel(sheetIndex: Int, type: Class<T>): Excel<T> {
            return Excel(sheetIndex, ValuesConverter.Excel(TypeValues(type)))
        }

        private fun rangeOf(start: Int, before: Int): IntArray {
            return (start until before).toList().toIntArray()
        }
    }
}
