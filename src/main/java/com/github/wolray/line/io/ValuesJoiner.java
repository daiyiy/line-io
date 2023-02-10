package com.github.wolray.line.io;

import com.github.wolray.seq.ArraySeq;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.github.wolray.line.io.TypeValues.invoke;

/**
 * @author wolray
 */
public class ValuesJoiner<T> {
    private final TypeValues<T> typeValues;
    private final ArraySeq<TypeValues.Attr> attrs;

    public ValuesJoiner(TypeValues<T> typeValues) {
        this.typeValues = typeValues;
        attrs = typeValues.toAttrs();
        initFormatters();
    }

    private void initFormatters() {
        Function<Object, String> toString = Object::toString;
        attrs.supply(a -> a.formatter = toString);
        TypeValues.processSimpleMethods(typeValues.type, this::processMethod);
    }

    void processMethod(TypeValues.SimpleMethod simpleMethod) {
        Method method = simpleMethod.method;
        Class<?> paraType = simpleMethod.paraType;
        if (paraType != String.class && simpleMethod.returnType == String.class) {
            method.setAccessible(true);
            Function<Object, String> function = s -> (String)invoke(method, s);
            Fields fields = method.getAnnotation(Fields.class);
            Predicate<Field> predicate = FieldSelector.toPredicate(fields);
            attrs
                .filter(a -> predicate.test(a.field) && a.field.getType() == paraType)
                .supply(a -> a.formatter = function);
        }
    }

    String join(String sep) {
        return join(sep, a -> a.field.getName());
    }

    String join(String sep, Function<TypeValues.Attr, String> function) {
        return attrs.join(sep, function);
    }

    public Function<T, String> toFormatter(String sep) {
        return t -> join(sep, a -> a.format(a.get(t)));
    }
}
