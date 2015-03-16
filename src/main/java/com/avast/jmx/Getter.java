package com.avast.jmx;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Jan Kolena (originally: Tomas Rehak)
 */
public class Getter {
    private final Object obj;
    private final Field f;

    public static Getter newGetter(Object obj, Field f) {
        return new Getter(obj, f);
    }

    private Getter(Object obj, Field f) {
        this.obj = obj;
        this.f = f;
    }

    public Object get() throws IllegalArgumentException, InvocationTargetException {
        final Class<?> type = f.getType();
        if (type.getSimpleName().startsWith("Atomic")) {
            return getAtomic();
        } else {
            try {
                return f.get(obj);
            } catch (IllegalAccessException e) {
                throw new InvocationTargetException(e);
            }
        }
    }

    protected Object getAtomic() throws InvocationTargetException {
        try {
            if (f.getType().equals(AtomicReference.class)) {
                final Method method = f.getType().getMethod("toString");
                return method.invoke(f.get(obj));
            }

            final Method method = f.getType().getMethod("get");
            return method.invoke(f.get(obj));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new InvocationTargetException(e);
        }
    }
}
