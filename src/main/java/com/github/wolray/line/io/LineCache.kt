package com.github.wolray.line.io

import com.github.wolray.seq.Seq
import com.github.wolray.seq.SeqCache
import java.io.File

/**
 * @author wolray
 */
abstract class LineCache<T>(val path: String) : SeqCache<T> {
    protected val cacheFile = File(path)
    override fun exists(): Boolean = cacheFile.exists()

    companion object {
        @JvmStatic
        @JvmOverloads
        fun <T> byCsv(file: String, type: Class<T>, sep: String = ","): LineCache<T> {
            return byCsv(file, DataMapper.from(type, sep))
        }

        @JvmStatic
        fun <T> byCsv(file: String, mapper: DataMapper<T>) = object : LineCache<T>("$file.csv") {
            override fun read(): Seq<T> = mapper.toReader().read(cacheFile).skipLines(1).toSeq()
            override fun write(ts: Iterable<T>) = mapper.toWriter().write(ts::forEach).autoHeader().toFile(path)
        }
    }
}
