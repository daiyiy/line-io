package com.github.wolray.line.logger

import com.github.wolray.line.io.CommonWriter.Companion.commonWriter
import org.slf4j.Logger
import java.util.function.Supplier
import java.util.function.UnaryOperator

/**
 * @author wolray
 */
class OneLogger(val log: Logger, val cls: String) : Logger by log {

    private fun get(): LogInOne.One? {
        val thread = Thread.currentThread()
        val one = LogInOne.oneMap[thread.name]
        if (one != null) {
            return one
        } else {
            val parent = LogInOne.oneMap[thread.threadGroup.name]
            if (parent != null) {
                return null
            }
            synchronized(thread) {
                return LogInOne.oneMap.getOrPut(thread.name) {
                    LogInOne.One(thread, StringBuilder().commonWriter())
                }
            }
        }
    }

    override fun debug(message: String?) {
        get()?.log(DEBUG, cls, message)
        log.debug(message)
    }

    override fun debug(format: String?, obj: Any?) {
        get()?.log(DEBUG, cls, format, obj)
        log.debug(format, obj)
    }

    override fun debug(format: String?, obj1: Any?, obj2: Any?) {
        get()?.log(DEBUG, cls, format, obj1, obj2)
        log.debug(format, obj1, obj2)
    }

    override fun debug(format: String?, vararg objects: Any?) {
        get()?.log(DEBUG, cls, format, *objects)
        log.debug(format, *objects)
    }

    override fun debug(message: String?, e: Throwable?) {
        get()?.log(DEBUG, cls, message, e)
        log.debug(message, e)
    }

    override fun info(message: String?) {
        get()?.log(INFO, cls, message)
        log.info(message)
    }

    override fun info(format: String?, obj: Any?) {
        get()?.log(INFO, cls, format, obj)
        log.info(format, obj)
    }

    override fun info(format: String?, obj1: Any?, obj2: Any?) {
        get()?.log(INFO, cls, format, obj1, obj2)
        log.info(format, obj1, obj2)
    }

    override fun info(format: String?, vararg objects: Any?) {
        get()?.log(INFO, cls, format, *objects)
        log.info(format, *objects)
    }

    override fun info(message: String?, e: Throwable?) {
        get()?.log(INFO, cls, message, e)
        log.info(message, e)
    }

    override fun warn(message: String?) {
        get()?.log(WARN, cls, message)
        log.warn(message)
    }

    override fun warn(format: String?, obj: Any?) {
        get()?.log(WARN, cls, format, obj)
        log.warn(format, obj)
    }

    override fun warn(format: String?, obj1: Any?, obj2: Any?) {
        get()?.log(WARN, cls, format, obj1, obj2)
        log.warn(format, obj1, obj2)
    }

    override fun warn(format: String?, vararg objects: Any?) {
        get()?.log(WARN, cls, format, *objects)
        log.warn(format, *objects)
    }

    override fun warn(message: String?, e: Throwable?) {
        get()?.log(WARN, cls, message, e)
        log.warn(message, e)
    }

    override fun error(message: String?) {
        get()?.log(ERROR, cls, message)
        log.error(message)
    }

    override fun error(format: String?, obj: Any?) {
        get()?.log(ERROR, cls, format, obj)
        log.error(format, obj)
    }

    override fun error(format: String?, obj1: Any?, obj2: Any?) {
        get()?.log(ERROR, cls, format, obj1, obj2)
        log.error(format, obj1, obj2)
    }

    override fun error(format: String?, vararg objects: Any?) {
        get()?.log(ERROR, cls, format, *objects)
        log.error(format, *objects)
    }

    override fun error(message: String?, e: Throwable?) {
        get()?.log(ERROR, cls, message, e)
        log.error(message, e)
    }

    fun dump(): String {
        return get()?.let {
            LogInOne.oneMap.remove(it.thread.name)
            it.close()
        }.orEmpty()
    }

    fun timing(message: String, runnable: Runnable) {
        timing(this, message, runnable)
    }

    fun <T> timing(message: String, supplier: Supplier<T>): T {
        return timing(this, message, supplier)
    }

    fun <T> timing(message: String): UnaryOperator<Supplier<T>> {
        return UnaryOperator {
            Supplier { timing(message, it) }
        }
    }

    companion object {
        private const val DEBUG = "DEBUG"
        private const val INFO = "INFO"
        private const val WARN = "WARN"
        private const val ERROR = "ERROR"

        @JvmStatic
        fun timing(log: Logger, message: String, runnable: Runnable) {
            timing<Any>(log, message) {
                runnable.run()
                runnable
            }
        }

        @JvmStatic
        fun <T> timing(log: Logger, message: String, supplier: Supplier<T>): T {
            log.info(message)
            val tic = System.currentTimeMillis()
            val res = supplier.get()
            log.info("{} done in {}ms", message, System.currentTimeMillis() - tic)
            return res
        }

        @JvmStatic
        fun <T> timing(log: Logger, message: String): UnaryOperator<Supplier<T>> {
            return UnaryOperator {
                Supplier { timing(log, message, it) }
            }
        }
    }
}
