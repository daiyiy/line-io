package com.github.wolray.line.io

import com.github.wolray.seq.IntSeq
import com.github.wolray.seq.Seq
import com.github.wolray.seq.WithCe
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method

/**
 * @author wolray
 */
object IteratorScope {
    fun <T, E> Iterator<T>.map(mapper: (T) -> E) = object : Iterator<E> {
        override fun hasNext(): Boolean = this@map.hasNext()
        override fun next(): E = mapper(this@map.next())
    }
}

object SeqScope {
    fun <T> Iterable<T>.seq(): Seq<T> = Seq.of(this)
    fun <T> Array<T>.seq(): Seq<T> = Seq.of(*this)
    fun IntArray.seq(): IntSeq = IntSeq.of(*this)
    inline fun <reified T> Seq<T>.toArray(): Array<T> = toObjArray { arrayOfNulls(it) }
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

object EmptyScope {
    inline fun <T> T.ifNotEmpty(block: T.() -> Unit) {
        val b = when (this) {
            is String -> isNotEmpty()
            is Array<*> -> isNotEmpty()
            is IntArray -> isNotEmpty()
            is Collection<*> -> isNotEmpty()
            is Map<*, *> -> isNotEmpty()
            is DoubleArray -> isNotEmpty()
            is LongArray -> isNotEmpty()
            else -> true
        }
        if (b) apply(block)
    }
}

object TypeScope {
    inline fun <reified T : Any> Class<*>.isType(checkPrimitive: Boolean = false): Boolean =
        this == T::class.javaObjectType || checkPrimitive && this == T::class.javaPrimitiveType

    inline fun <E : Class<out Exception>, T> E?.ignorableToCall(crossinline block: () -> T): T? {
        return WithCe.call(this) { block() }
    }
}