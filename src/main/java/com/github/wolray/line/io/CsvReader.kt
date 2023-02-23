package com.github.wolray.line.io

import com.github.wolray.seq.IsReader
import com.github.wolray.seq.IsReader.InputSource
import com.github.wolray.seq.Seq
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.function.Consumer

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

    private fun BufferedReader.supply(c: Consumer<List<String>>) {
        while (true) {
            val s = readLine()
            if (s != null) c.accept(split(s)) else break
        }
    }

    private fun BufferedReader.supplyRemovingMarker(c: Consumer<List<String>>) {
        var rest = false
        while (true) {
            val s = if (rest) readLine() else {
                rest = true
                readLine().removePrefix(CsvWriter.utf8Marker.toString())
            }
            if (s != null) c.accept(split(s)) else break
        }
    }

    override fun toSeq(source: InputSource): Seq<List<String>> = Seq { c ->
        BufferedReader(InputStreamReader(source.call())).use {
            if (skip > 0) it.supply(c) else it.supplyRemovingMarker(c)
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