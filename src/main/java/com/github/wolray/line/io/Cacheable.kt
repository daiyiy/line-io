package com.github.wolray.line.io

import java.io.File
import java.io.FileInputStream

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

    private fun cacheFile(
        file: String, suffix: String,
        reader: () -> LineReader.Text<T>,
        writer: () -> LineWriter<T>
    ): S {
        val path = file + suffix
        val f = File(path)
        return cacheBy(object : Cache<T> {
            override fun exists() = f.exists()

            override fun read() = reader.invoke().read { FileInputStream(f) }.also {
                if (it is CsvReader<T>.Session) {
                    it.skipLines(1)
                }
            }

            override fun write(ts: Iterable<T>) = writer.invoke().write(path).also {
                if (it is CsvWriter.Session) {
                    it.autoHeader()
                }
            }.with(ts)
        })
    }

    @JvmOverloads
    fun cacheCsv(file: String, type: Class<T>, sep: String = ","): S {
        return cacheCsv(file, DataMapper.simple(type, sep))
    }

    fun cacheCsv(file: String, mapper: DataMapper<T>): S {
        return cacheFile(file, ".csv", mapper::toReader, mapper::toWriter)
    }

    fun cacheJson(file: String, type: Class<T>): S {
        return cacheFile(file, ".txt", { LineReader.byJson(type) }, { LineWriter.byJson() })
    }

    interface Cache<T> {
        fun exists(): Boolean
        fun read(): LineReader<*, *, T>.Session
        fun write(ts: Iterable<T>)
    }
}
