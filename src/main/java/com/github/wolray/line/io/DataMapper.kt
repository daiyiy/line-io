package com.github.wolray.line.io

import java.lang.reflect.Field
import java.util.function.Function

/**
 * @author wolray
 */
class DataMapper<T> @JvmOverloads constructor(
    val typeValues: TypeValues<T>,
    val sep: String = DEFAULT_SEP
) {
    private val converter by lazy { ValuesConverter.Csv(typeValues) }
    private val joiner by lazy { ValuesJoiner(typeValues) }
    private val parser by lazy { toParser(sep) }
    private val formatter by lazy { toFormatter(sep) }

    fun newSep(sep: String): DataMapper<T> {
        return if (sep == this.sep) this else DataMapper(typeValues, sep)
    }

    fun toParser(sep: String): Function<String, T> = converter.toParser(sep)
    fun toFormatter(sep: String): Function<T, String> = joiner.toFormatter(sep)

    @JvmOverloads
    fun joinFields(sep: String = this.sep): String = joiner.joinFields(sep)

    @JvmOverloads
    fun toReader(sep: String = this.sep) = LineReader.Csv(sep, converter)

    @JvmOverloads
    fun toWriter(sep: String = this.sep) = CsvWriter(joiner, sep)

    fun parse(s: String) = parser.apply(s)
    fun format(t: T) = formatter.apply(t)

    class Builder<T> internal constructor(private val type: Class<T>) {
        var pojo: Boolean = false
        var use: Array<String>? = null
        var omit: Array<String>? = null
        var useRegex: String? = null
        var omitRegex: String? = null

        private fun toFields() = Fields(
            pojo = pojo, use = use ?: emptyArray(), omit = omit ?: emptyArray(),
            useRegex = useRegex.orEmpty(), omitRegex = omitRegex.orEmpty()
        )

        fun pojo() = apply { pojo = true }
        fun use(vararg fields: String) = apply { use = arrayOf(*fields) }
        fun omit(vararg fields: String) = apply { omit = arrayOf(*fields) }
        fun useRegex(regex: String) = apply { useRegex = regex }
        fun omitRegex(regex: String) = apply { omitRegex = regex }
        fun build() = DataMapper(TypeValues(type, toFields()))
        fun build(sep: String) = DataMapper(TypeValues(type, toFields()), sep)
        fun toReader(sep: String) = build(sep).toReader()
        fun toWriter(sep: String) = build(sep).toWriter()
    }

    companion object {
        const val DEFAULT_SEP = "\u02cc"

        @JvmStatic
        @JvmOverloads
        @Deprecated("Bad name, use from instead", ReplaceWith("from"))
        fun <T> simple(type: Class<T>, sep: String = DEFAULT_SEP) = DataMapper(TypeValues(type), sep)

        @JvmStatic
        @JvmOverloads
        fun <T> from(type: Class<T>, sep: String = DEFAULT_SEP) = DataMapper(TypeValues(type), sep)

        @JvmStatic
        @Deprecated("Bad name, use builder instead", ReplaceWith("builder"))
        fun <T> of(type: Class<T>) = Builder(type)

        @JvmStatic
        fun <T> builder(type: Class<T>) = Builder(type)

        fun Fields?.toTest(): (Field) -> Boolean {
            this ?: return { true }
            if (use.isNotEmpty()) {
                val set = use.toSet()
                return { set.contains(it.name) }
            }
            if (omit.isNotEmpty()) {
                val set = omit.toSet()
                return { !set.contains(it.name) }
            }
            if (useRegex.isNotEmpty()) {
                val regex = useRegex.toRegex()
                return { it.name.matches(regex) }
            }
            if (omitRegex.isNotEmpty()) {
                val regex = omitRegex.toRegex()
                return { !it.name.matches(regex) }
            }
            return { true }
        }
    }
}
