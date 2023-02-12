package com.github.wolray.line.io

import com.github.wolray.seq.IsReader
import com.github.wolray.seq.Seq
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * @author wolray
 */
class CsvReader<T> internal constructor(val sep: String, valuesConverter: ValuesConverter.Csv<T>) :
    LineReader<IsReader.InputSource, List<String>, T>(valuesConverter),
    IsReader<LineReader<IsReader.InputSource, List<String>, T>.Session> {

    override fun splitHeader(v: List<String>): List<String> = v

    override fun toIterator(source: IsReader.InputSource): Iterator<List<String>> {
        return BufferedReader(InputStreamReader(source.call()))
            .lineSequence()
            .map { it.split(sep, limit = limit) }
            .iterator()
    }

    override fun toSeq(source: IsReader.InputSource): Seq<List<String>> = Seq {
        BufferedReader(InputStreamReader(source.call())).use { reader ->
            while (true) {
                val s = reader.readLine()
                if (s != null) it.accept(s.split(sep, limit = limit)) else break
            }
        }
    }

    companion object {
        @JvmStatic
        fun <T> of(sep: String, type: Class<T>): CsvReader<T> {
            return CsvReader(sep, ValuesConverter.Csv(TypeValues(type)))
        }

        @JvmStatic
        fun <T> of(sep: String, dataMapper: DataMapper<T>): CsvReader<T> {
            return CsvReader(sep, ValuesConverter.Csv(dataMapper.typeValues))
        }
    }
}