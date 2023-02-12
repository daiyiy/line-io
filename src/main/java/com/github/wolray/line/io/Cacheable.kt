package com.github.wolray.line.io

import java.io.File

/**
 * @author wolray
 */
abstract class Cacheable<T, S> {
    abstract fun from(session: LineReader<*, *, T>.Session): S
    abstract fun toList(): List<T>
    abstract fun after(): S

    fun cacheBy(cache: Cache<T>): S {
        return if (cache.exists()) {
            from(cache.read())
        } else {
            toList().also { if (it.isNotEmpty()) cache.write(it) }
            after()
        }
    }

    @JvmOverloads
    @Deprecated("Use toSeq().cacheBy(LineCache.byCsv)", ReplaceWith("cacheBy"))
    fun cacheCsv(file: String, type: Class<T>, sep: String = ","): S {
        return cacheCsv(file, DataMapper.from(type, sep))
    }

    @Deprecated("Use toSeq().cacheBy(LineCache.byCsv)", ReplaceWith("cacheBy"))
    fun cacheCsv(file: String, mapper: DataMapper<T>): S {
        val path = "$file.csv"
        val f = File(path)
        return cacheBy(object : Cache<T> {
            override fun exists() = f.exists()
            override fun read() = mapper.toReader().read(f).skipLines(1)
            override fun write(ts: Iterable<T>) = mapper.toWriter().write(path).autoHeader().with(ts)
        })
    }

    interface Cache<T> {
        fun exists(): Boolean
        fun read(): LineReader<*, *, T>.Session
        fun write(ts: Iterable<T>)
    }
}
