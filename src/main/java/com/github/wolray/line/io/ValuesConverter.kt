package com.github.wolray.line.io

import com.github.wolray.line.io.DataMapper.Companion.toTest
import com.github.wolray.line.io.MethodScope.annotation
import com.github.wolray.line.io.MethodScope.asMapper
import com.github.wolray.line.io.MethodScope.catchAsNull
import com.github.wolray.line.io.SeqScope.seq
import com.github.wolray.line.io.SeqScope.toArray
import com.github.wolray.line.io.TypeScope.isType
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.util.function.Function
import kotlin.math.min

/**
 * @author wolray
 */
abstract class ValuesConverter<V, E, T>(val typeValues: TypeValues<T>) : (V) -> T {
    val attrs: Array<Attr> = typeValues.map(::toAttr).toArray()
    private val constructor: Constructor<T> = typeValues.type.getConstructor()
    private var filler: (T, V) -> Unit

    init {
        filler = fillAll()
        TypeValues.processSimpleMethods(typeValues.type, ::processMethod)
    }

    abstract fun sizeOf(values: V): Int
    abstract fun getAt(values: V, slot: Int): E

    fun toChar(e: E): Char = toStr(e)[0]
    abstract fun toStr(e: E): String
    abstract fun toBool(e: E): Boolean
    abstract fun toInt(e: E): Int
    abstract fun toDouble(e: E): Double
    abstract fun toLong(e: E): Long

    private fun toAttr(f: Field): Attr {
        val t = f.type
        val mapper = when {
            t.isType<Char>(true) -> ::toChar
            t.isType<String>() -> ::toStr
            t.isType<Boolean>(true) -> ::toBool
            t.isType<Int>(true) -> ::toInt
            t.isType<Double>(true) -> ::toDouble
            t.isType<Long>(true) -> ::toLong
            else -> unknownMapper(t)
        }
        return Attr(f, mapper)
    }

    private fun unknownMapper(t: Class<*>): (E) -> Any? = {
        throw IllegalStateException(
            "cannot parse $t, please add a static method " +
                "(String -> ${t.simpleName}) inside ${typeValues.type.simpleName}"
        )
    }

    private fun convertAt(values: V, slot: Int, attr: Attr): Any? = catchAsNull {
        attr.mapper(getAt(values, slot))
    }

    open fun processMethod(method: TypeValues.SimpleMethod) {}

    override fun invoke(v: V): T {
        val t = constructor.newInstance()
        filler(t, v)
        return t
    }

    fun resetOrder(slots: IntArray) {
        filler = fillBySlots(slots)
    }

    private fun fillAll(): (T, V) -> Unit {
        val len = typeValues.size
        return { t, v ->
            val max = min(len, sizeOf(v))
            for (i in 0 until max) {
                fillAt(t, v, i, attrs[i])
            }
        }
    }

    private fun fillBySlots(slots: IntArray): (T, V) -> Unit {
        val len = min(typeValues.size, slots.size)
        return { t, v ->
            val max = min(len, sizeOf(v))
            for (i in 0 until max) {
                fillAt(t, v, slots[i], attrs[i])
            }
        }
    }

    private fun fillAt(t: T, values: V, slot: Int, attr: Attr) {
        attr.field[t] = convertAt(values, slot, attr)
    }

    inner class Attr(val field: Field, var mapper: (E) -> Any?)

    class Csv<T>(typeValues: TypeValues<T>) : ValuesConverter<List<String>, String, T>(typeValues) {

        override fun sizeOf(values: List<String>): Int = values.size
        override fun getAt(values: List<String>, slot: Int): String = values[slot]

        override fun toStr(e: String): String = e
        override fun toBool(e: String): Boolean = e.toBoolean()
        override fun toInt(e: String): Int = e.toInt()
        override fun toDouble(e: String): Double = e.toDouble()
        override fun toLong(e: String): Long = e.toLong()

        override fun processMethod(method: TypeValues.SimpleMethod) {
            val m = method.method
            val returnType = m.returnType
            if (method.paraType.isType<String>()) {
                val test = m.annotation<Fields>().toTest()
                val seq = attrs.seq().filter { a -> test(a.field) }
                if (returnType.isType<String>()) {
                    val mapper = m.asMapper<String, String>("")
                    seq.supply { a ->
                        val old = a.mapper
                        a.mapper = { s -> old(mapper(s)) }
                    }
                } else {
                    val mapper = m.asMapper<String, Any>()
                    seq
                        .filter { a -> a.field.type == returnType }
                        .supply { a -> a.mapper = mapper }
                }
            }
        }

        fun toParser(sep: String): Function<String, T> {
            val limit = typeValues.size + 1
            return Function { this(it.split(sep, limit = limit)) }
        }
    }

    class Excel<T>(typeValues: TypeValues<T>) : ValuesConverter<Row, Cell, T>(typeValues) {

        override fun sizeOf(values: Row): Int = values.lastCellNum.toInt()
        override fun getAt(values: Row, slot: Int): Cell = values.getCell(slot)

        override fun toStr(e: Cell): String = e.stringCellValue
        override fun toBool(e: Cell): Boolean = e.booleanCellValue
        override fun toInt(e: Cell): Int = e.numericCellValue.toInt()
        override fun toDouble(e: Cell): Double = e.numericCellValue
        override fun toLong(e: Cell): Long = e.numericCellValue.toLong()
    }
}