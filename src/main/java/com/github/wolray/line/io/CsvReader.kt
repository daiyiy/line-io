package com.github.wolray.line.io

/**
 * @author wolray
 */
@Deprecated("Unnecessary, use LineReader.byCsv instead")
class CsvReader<T> internal constructor(sep: String, converter: ValuesConverter.Csv<T>) :
    LineReader.Csv<T>(sep, converter) {

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