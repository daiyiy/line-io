package com.github.wolray.line.logger

import com.github.wolray.line.io.CommonWriter
import org.slf4j.Logger
import org.slf4j.helpers.MessageFormatter
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/**
 * @author wolray
 */
object LogInOne {
    private val SHORT_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")
    private const val DASH = "----"
    @JvmStatic
    val oneMap: MutableMap<String, One> = ConcurrentHashMap()

    @JvmStatic
    fun getLogger(log: Logger, cls: Class<*>): OneLogger {
        return OneLogger(log, cls.simpleName)
    }

    class One(
        internal val thread: Thread,
        private val writer: CommonWriter<StringBuilder>
    ) {
        private var lastTag: String? = null

        private fun writeElement(tag: String) {
            writer.write(tag)
            writer.write(' ')
        }

        private fun writeHead(level: String, cls: String) {
            writeElement(LocalDateTime.now().format(SHORT_TIME))
            val tag = "$level [${thread.name}] $cls"
            if (tag == lastTag) {
                writeElement(DASH)
            } else {
                lastTag = tag
                writeElement(tag)
                writer.write(": ")
            }
        }

        fun log(level: String, cls: String, format: String?, vararg objects: Any?) {
            if (format != null) {
                writeHead(level, cls)
                if (objects.isEmpty()) {
                    writer.writeLine(format)
                } else {
                    writer.writeLine(MessageFormatter.arrayFormat(format, objects).message)
                }
            }
        }

        fun log(level: String, cls: String, message: String?, e: Throwable?) {
            writeHead(level, cls)
            if (message != null) {
                writeElement(message)
            }
            if (e != null) {
                val stringWriter = StringWriter()
                e.printStackTrace(PrintWriter(stringWriter))
                writer.writeLine(stringWriter.toString());
            }
        }

        fun close(): String {
            return writer.backer.toString()
        }
    }
}
