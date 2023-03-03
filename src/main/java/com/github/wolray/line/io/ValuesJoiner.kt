package com.github.wolray.line.io

import com.github.wolray.line.io.DataMapper.Companion.toTest
import com.github.wolray.line.io.MethodScope.annotation
import com.github.wolray.line.io.MethodScope.asMapper
import com.github.wolray.line.io.TypeScope.isType
import com.github.wolray.line.io.TypeValues.SimpleMethod
import com.github.wolray.seq.ArraySeq
import java.lang.reflect.Field
import java.util.function.Function

/**
 * @author wolray
 */
class ValuesJoiner<T>(typeValues: TypeValues<T>) {
    val attrs: ArraySeq<Attr> = typeValues.map { it -> Attr(it, mapper) }.toList()

    init {
        TypeValues.processSimpleMethods(typeValues.type, ::processMethod)
    }

    private fun processMethod(simpleMethod: SimpleMethod) {
        val method = simpleMethod.method
        val paraType = simpleMethod.paraType
        if (paraType.isType<String>().not() && simpleMethod.returnType.isType<String>()) {
            val mapper = method.asMapper<Any?, String>("")
            val test = method.annotation<Fields>().toTest()
            attrs
                .filter { a -> test(a.field) && a.field.type == paraType }
                .supply { a -> a.mapper = mapper }
        }
    }

    fun toFormatter(sep: String): Function<T, String> = Function { t ->
        join(sep) { it.mapper(it.field[t]) }
    }

    fun joinFields(sep: String) = join(sep) { it.field.name }

    fun join(sep: String, mapper: (Attr) -> String) = attrs.joinToString(sep, transform = mapper)

    class Attr(val field: Field, var mapper: (Any?) -> String)

    companion object {
        val mapper = { o: Any? -> o?.toString().orEmpty() }
    }
}