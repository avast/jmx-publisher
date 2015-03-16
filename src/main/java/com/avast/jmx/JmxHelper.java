package com.avast.jmx;

import java.lang.reflect.Field;

/**
 * Created <b>18.11.13</b><br>
 *
 * @author Jenda Kolena, kolena@avast.com
 * @version 1.0
 */
abstract class JmxHelper {//package visibility

    private JmxHelper() {
    }

    /**
     * Converts value given as <code>String</code> to specified dataType. Following types are supported:
     * <ul>
     * <li>Integer, int</li>
     * <li>Long, long</li>
     * <li>Double, double</li>
     * <li>Float, float</li>
     * <li>Boolean, boolean</li>
     * <li>Byte, byte</li>
     * <li>Character, char</li>
     * <li>String</li>
     * </ul>
     *
     * @param value The value to be converted.
     * @param clazz The future class of the value.
     * @return The value.
     */
    public static Object convertValue(String value, Class clazz) {
        assert clazz != null && value != null;

        if (clazz.equals(Integer.TYPE) || clazz.equals(Integer.class)) {
            return Integer.parseInt(value);
        }
        if (clazz.equals(Double.TYPE) || clazz.equals(Double.class)) {
            return Double.parseDouble(value);
        }
        if (clazz.equals(String.class)) {
            return value;
        }
        if (clazz.equals(Boolean.TYPE) || clazz.equals(Boolean.class)) {
            return Boolean.parseBoolean(value);
        }
        if (clazz.equals(Byte.TYPE) || clazz.equals(Byte.class)) {
            return Byte.parseByte(value);
        }
        if (clazz.equals(Long.TYPE) || clazz.equals(Long.class)) {
            return Long.parseLong(value);
        }
        if (clazz.equals(Float.TYPE) || clazz.equals(Float.class)) {
            return Float.parseFloat(value);
        }
        if (clazz.equals(Character.TYPE) || clazz.equals(Character.class)) {
            return value.charAt(0);
        }

        throw new IllegalArgumentException("Unsupported class: " + clazz);
    }

    /**
     * Gets basic equiv for some atomic class (one of AtomicInteger, AtomicLong, AtomicBoolean).
     *
     * @param f The field.
     * @return The basic class.
     */
    public static Class getBasicTypeForAtomic(Field f) {
        if (!f.getType().getName().startsWith("java.util.concurrent.atomic")) {
            throw new IllegalArgumentException("Only atomic classes supported");
        }

        String cl = f.getType().getSimpleName().replaceAll("Atomic", "").toLowerCase();

        switch (cl) {
            case "boolean":
                return Boolean.TYPE;
            case "integer":
                return Integer.TYPE;
            case "long":
                return Long.TYPE;
            default:
                throw new IllegalArgumentException("Unsupported class: " + f.getType());
        }
    }
}
