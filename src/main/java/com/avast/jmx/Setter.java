package com.avast.jmx;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Jan Kolena (originally: Tomas Rehak)
 */
public class Setter {
    private final Object obj;
    private final Field f;

    public static Setter newSetter(Object obj, Field f) {
        return new Setter(obj, f);
    }

    private Setter(Object obj, Field f) {
        this.obj = obj;
        this.f = f;
    }

    public void set(Object val) throws IllegalArgumentException, InvocationTargetException {
        final Class<?> type = f.getType();
        if (type.getSimpleName().startsWith("Atomic")) {
            setAtomic(val);
        } else {
            try {
                f.set(obj, JmxHelper.convertValue(val.toString(), type));
            } catch (IllegalAccessException e) {
                throw new InvocationTargetException(e);
            }
        }
    }

    protected void setAtomic(Object val) throws InvocationTargetException {
        final Class cl = JmxHelper.getBasicTypeForAtomic(f);

        try {
            final Method method = f.getType().getMethod("set", cl);
            method.invoke(f.get(obj), JmxHelper.convertValue(val.toString(), cl));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new InvocationTargetException(e);
        }
    }
}
