package com.github.wolray.line.io

import com.github.wolray.seq.Seq
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method

/**
 * @author wolray
 */
object SeqScope {
    fun <T> Array<T>.seq(): Seq<T> = Seq.of(*this)
    fun <T> Iterable<T>.seq(): Seq<T> = Seq.of(this)
    inline fun <reified T> Seq<T>.toArray(): Array<T> =
        toObjArray { arrayOfNulls(it) }
}

object MethodScope {
    inline fun <T> catchAsNull(block: () -> T): T? = try {
        block()
    } catch (e: Throwable) {
        null
    }

    inline fun <reified T : Annotation> AnnotatedElement.annotation(): T? = getAnnotation(T::class.java)

    fun <A, B> Method.asMapper(): (A) -> B? {
        isAccessible = true
        return { call(it) }
    }

    fun <A, B> Method.asMapper(default: B): (A) -> B {
        isAccessible = true
        return { call(it) ?: default }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <A, B> Method.call(a: A?): B? {
        a ?: return null
        return catchAsNull { invoke(null, a) as? B }
    }
}

object TypeScope {
    inline fun <reified T : Any> Class<*>.isType(checkPrimitive: Boolean = false): Boolean =
        this == T::class.javaObjectType || checkPrimitive && this == T::class.javaPrimitiveType
}