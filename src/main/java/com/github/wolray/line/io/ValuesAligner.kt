package com.github.wolray.line.io

/**
 * @author wolray
 */
abstract class ValuesAligner<V, T>(val converter: ValuesConverter<V, *, T>) {
    internal var limit = 0
    private var slots: IntArray? = null
    private var cols: Array<String>? = null

    fun columns(vararg col: String) = apply { cols = arrayOf(*col) }
    fun columns(vararg index: Int) = apply { slots = index }
    fun columnsBefore(index: Int) = columnsRange(0, index)

    fun columnsRange(start: Int, before: Int) = apply {
        slots = (start until before).toList().toIntArray()
    }

    fun excelColumns(excelCols: String) = apply {
        slots = if (excelCols.isEmpty()) intArrayOf()
        else excelCols.split(",").map(::excelIndex).toIntArray()
    }

    private fun excelIndex(col: String): Int {
        return col.trim().fold(0) { acc, c -> acc * 26 + (c - A + 1) } - 1
    }

    abstract fun splitHeader(v: V): List<String>

    fun preprocess(v: V): Boolean {
        var res = false
        cols?.apply {
            if (isNotEmpty()) {
                val split = splitHeader(v)
                slots = toSlots(split)
                res = true
            }
        }
        limit = limit.coerceAtLeast(converter.typeValues.size + 1)
        slots?.apply {
            if (isNotEmpty()) {
                converter.resetOrder(this)
                limit = limit.coerceAtLeast(max() + 2)
            }
        }
        return res
    }

    companion object {
        private const val A = 'A'

        fun Array<String>.toSlots(split: List<String>): IntArray = map {
            split.indexOf(it).apply {
                if (this < 0) throw NoSuchElementException("$it not in $split")
            }
        }.toIntArray()
    }
}