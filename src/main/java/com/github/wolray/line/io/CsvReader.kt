package com.github.wolray.line.io

import com.github.wolray.seq.IsReader
import com.github.wolray.seq.IsReader.InputSource
import com.github.wolray.seq.Seq
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * @author wolray
 */
class CsvReader<T> internal constructor(val sep: String, converter: ValuesConverter.Csv<T>) :
    LineReader<InputSource, List<String>, T>(converter),
    IsReader<LineReader<InputSource, List<String>, T>.Session> {

    private fun split(s: String): List<String> = s.split(sep, limit = limit)

    override fun splitHeader(v: List<String>): List<String> = v

    override fun toIterator(source: InputSource): Iterator<List<String>> {
        return BufferedReader(InputStreamReader(source.call()))
            .lineSequence()
            .map(::split)
            .iterator()
    }

    override fun toSeq(source: InputSource): Seq<List<String>> = Seq {
        BufferedReader(InputStreamReader(source.call())).use { reader ->
            while (true) {
                val s = reader.readLine()
                if (s != null) it.accept(split(s)) else break
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