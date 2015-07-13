package com.avast.jmx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.openmbean.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Jan Kolena - kolena@avast.com (originally: Tomas Rehak)
 */
@SuppressWarnings("unused")
public class Property {
    private static final Logger LOGGER = LoggerFactory.getLogger(Property.class);
    private Field field;
    private String name;
    private String desc;
    private boolean readable;
    private boolean setable;
    private Method getter;
    private Method setter;
    private Object instance;
    private Object setterTarget;
    private Object getterTarget;
    private String type;
    private Class<?> originalClass;
    private boolean compositeDataWrapper = false;

    public Property(Object instance, Class<?> originalClass, Field f, String name, String desc, boolean readable, boolean setable, Method getter, Method setter) {
        this.instance = instance;
        this.getter = getter;
        this.setter = setter;
        this.name = name;
        this.desc = desc;
        this.readable = readable;
        this.setable = setable;
        this.field = f;
        this.getterTarget = instance;
        this.setterTarget = instance;
        this.originalClass = originalClass;
        openTypeConversionCheck(f);
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        if (compositeDataWrapper) {
            return CompositeData.class.getName();
        } else {
            return type;
        }
    }

    public Object getValue() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        MyPreconditions.checkArgument(readable);
        MyPreconditions.checkNotNull(getter);
        MyPreconditions.checkNotNull(getterTarget);
        if (!getter.isAccessible()) getter.setAccessible(true);
        return convert(getter.invoke(getterTarget));
    }

    public Object getSetterTarget() {
        return setterTarget;
    }

    public void setSetterTarget(Object setterTarget) {
        this.setterTarget = setterTarget;
    }

    public Object getGetterTarget() {
        return getterTarget;
    }

    public void setGetterTarget(Object getterTarget) {
        this.getterTarget = getterTarget;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public boolean isReadable() {
        return readable;
    }

    public void setReadable(boolean readable) {
        this.readable = readable;
    }

    public boolean isSetable() {
        return setable;
    }

    public void setSetable(boolean setable) {
        this.setable = setable;
    }

    public Method getGetter() {
        return getter;
    }

    public void setGetter(Method getter) {
        this.getter = getter;
    }

    public Method getSetter() {
        return setter;
    }

    public void setSetter(Method setter) {
        this.setter = setter;
    }

    public void setValue(Object val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        MyPreconditions.checkArgument(setable);
        MyPreconditions.checkNotNull(setter);
        MyPreconditions.checkNotNull(setterTarget);
        if (!setter.isAccessible()) setter.setAccessible(true);
        setter.invoke(setterTarget, val);
    }

    public Class<?> getOriginalClass() {
        return originalClass;
    }

    public void setOriginalClass(Class<?> originalClass) {
        this.originalClass = originalClass;
    }

    @Override
    public String toString() {
        return "Property{" + "field=" + field + ", name=" + name + ", desc=" + desc + ", readable=" + readable + ", setable=" + setable + ", getter=" + getter + ", setter=" + setter + ", instance=" + instance + ", setterTarget=" + setterTarget + ", getterTarget=" + getterTarget + ", type=" + type + '}';
    }

    private void openTypeConversionCheck(Field field) {
        if (field != null) { //the field can be null for properties backed by methods
            final Class<?> fieldType = field.getType();
            if (fieldType.equals(Map.class)) {
                compositeDataWrapper = true;
                setable = false; // Really do not want to set CompositeData
            }
        }
    }

    /**
     * Method will convert following classes:
     * <ul>
     * <li>{@link Map}</li>
     * </ul>
     * <p/>
     * to new {@link CompositeData} representation
     *
     * @return original obj or converted obj
     */
    private Object convert(Object obj) {
        if (compositeDataWrapper && (obj instanceof Map)) {
            return convertMapToCompositeData((Map) obj);
        }
        // do not need to convert data || unable to convert data
        return obj;
    }

    /**
     * Convert {@link Map} to {@link CompositeData} obj
     *
     * @param inputMap map which will be converted to {@link CompositeData}
     * @return new representation of map in CompositeData obj OR
     * input Map if not possible
     */
    private Object convertMapToCompositeData(Map inputMap) {
        final int size = inputMap != null ? inputMap.size() : 0;

        if (size == 0) {
            LOGGER.debug("Exposing empty map");
            return null;
        }

        final String[] itemNames = new String[size];
        final String[] itemDescs = new String[size];
        final OpenType[] itemTypes = new OpenType[size];
        final Object[] itemValues = new Object[size];

        int i = 0;

        for (Object key : inputMap.keySet()) {
            final String keyValue = key.toString();
            final Object value = inputMap.get(key);

            itemNames[i] = keyValue;
            itemDescs[i] = keyValue;
            itemValues[i] = simplifyValue(value);
            itemTypes[i] = getSimpleType(value);
            ++i;
        }

        try {
            final CompositeType compositeType = new CompositeType(name, "CompositeData wrapper-" + desc, itemNames, itemDescs, itemTypes);
            return new CompositeDataSupport(compositeType, itemNames, itemValues);
        } catch (OpenDataException e) {
            LOGGER.warn("Unable to convert map to open type!", e);
            return null;
        } catch (Exception e) {
            LOGGER.warn("Unknown problem while exposing the data", e);
            return null;
        }
    }

    private Object simplifyValue(final Object value) {
        final Object newValue;

        if (AtomicInteger.class.isInstance(value)) {
            newValue = AtomicInteger.class.cast(value).get();
        } else if (AtomicLong.class.isInstance(value)) {
            newValue = AtomicLong.class.cast(value).get();
        } else if (AtomicBoolean.class.isInstance(value)) {
            newValue = AtomicBoolean.class.cast(value).get();
        } else newValue = value;

        return newValue;
    }

    private SimpleType<?> getSimpleType(final Object value) {
        switch (value.getClass().getSimpleName().toLowerCase()) {
            case "integer":
            case "atomicinteger":
                return SimpleType.INTEGER;
            case "biginteger":
                return SimpleType.BIGINTEGER;
            case "long":
            case "atomiclong":
                return SimpleType.LONG;
            case "short":
                return SimpleType.SHORT;
            case "byte":
                return SimpleType.BYTE;
            case "date":
                return SimpleType.DATE;
            case "double":
                return SimpleType.DOUBLE;
            case "float":
                return SimpleType.FLOAT;
            case "boolean":
            case "atomicboolean":
                return SimpleType.BOOLEAN;

            default:
                LOGGER.debug("Unable to convert type of the value");
            case "string":
                return SimpleType.STRING;
        }
    }
}
