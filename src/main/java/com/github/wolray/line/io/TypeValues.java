package com.github.wolray.line.io;

import com.github.wolray.seq.ArraySeq;
import com.github.wolray.seq.Seq;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * @author wolray
 */
public class TypeValues<T> {
    public static final int STF = Modifier.STATIC | Modifier.FINAL | Modifier.TRANSIENT;
    public final Class<T> type;
    public final ArraySeq<Field> values;

    public TypeValues(Class<T> type) {
        this(type, null);
    }

    public TypeValues(Class<T> type, Fields fields) {
        this.type = type;
        values = getFields(type, fields)
            .filter(f -> (f.getModifiers() & STF) == 0)
            .filter(DataMapper.toTest(fields))
            .toList();
    }

    static Object invoke(Method method, Object o) {
        try {
            return method.invoke(null, o);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    static void processSimpleMethods(Class<?> type, Consumer<SimpleMethod> consumer) {
        for (Method m : type.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                Class<?>[] parameterTypes = m.getParameterTypes();
                Class<?> returnType = m.getReturnType();
                if (parameterTypes.length == 1 && returnType != void.class) {
                    consumer.accept(new SimpleMethod(m, parameterTypes[0], returnType));
                }
            }
        }
    }

    private static <S, T> T safeApply(Function<S, T> function, S s) {
        return safeApply(function, s, null);
    }

    private static <S, T> T safeApply(Function<S, T> function, S s, T defaultValue) {
        if (s != null) {
            try {
                return function.apply(s);
            } catch (Throwable ignore) {}
        }
        return defaultValue;
    }

    ArraySeq<Attr> toAttrs() {
        return values.map(Attr::new).toList();
    }

    private Seq<Field> getFields(Class<T> type, Fields fields) {
        if (fields != null && fields.pojo()) {
            return Seq.of(type.getDeclaredFields())
                .filter(f -> Modifier.isPrivate(f.getModifiers()))
                .onEach(f -> f.setAccessible(true));
        } else {
            return Seq.of(type.getFields());
        }
    }

    static class SimpleMethod {
        final Method method;
        final Class<?> paraType;
        final Class<?> returnType;

        SimpleMethod(Method method, Class<?> paraType, Class<?> returnType) {
            this.method = method;
            this.paraType = paraType;
            this.returnType = returnType;
        }
    }

    public static class Attr {
        private static final String EMPTY_STRING = "";
        public final Field field;
        UnaryOperator<String> mapper;
        Function<String, ?> parser;
        Function<Object, ?> function;
        Function<Object, String> formatter;

        Attr(Field field) {
            this.field = field;
        }

        void composeMapper() {
            if (mapper != null) {
                parser = parser.compose(mapper);
            }
        }

        public Object parse(String s) {
            return safeApply(parser, s);
        }

        public Object convert(Object o) {
            return safeApply(function, o);
        }

        public String format(Object o) {
            return safeApply(formatter, o, EMPTY_STRING);
        }

        void set(Object t, Object o) {
            try {
                field.set(t, o);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        Object get(Object t) {
            try {
                return field.get(t);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
