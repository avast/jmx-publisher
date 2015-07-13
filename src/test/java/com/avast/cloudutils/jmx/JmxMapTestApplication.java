package com.avast.cloudutils.jmx;

import com.avast.jmx.JMXProperty;
import com.avast.jmx.JMXPropertyGetter;
import com.avast.jmx.JMXPropertySetter;
import com.avast.jmx.MyDynamicBean;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by barina on 08/06/15.
 */
public class JmxMapTestApplication {

    enum Key {
        FIRST, SECOND, THIRD
    }

    @JMXProperty
    public int number = 0;

    @JMXProperty
    public Map<String, String> stringMap = new HashMap<>();

    @JMXProperty
    public Map<Integer, Integer> emptyMap = new HashMap<>();

    @JMXProperty
    public Map<Integer, Integer> integerMap = new HashMap<>();

    @JMXProperty
    public Map<Integer, AtomicInteger> atomicIntegerMap = new HashMap<>();

    @JMXProperty
    public Map<Key, Integer> enumIntegerMap = new HashMap<>();

    @JMXPropertyGetter
    public Map getEmptyMap() {
        return emptyMap;
    }

    @JMXPropertySetter
    public void setEmptyMap(Map<Integer, Integer> emptyMap) {
        this.emptyMap = emptyMap;
    }


    public JmxMapTestApplication() {
        MyDynamicBean.exposeAndRegisterSilently(this);
        stringMap.put("str1", "str1");
        stringMap.put("str2", "str2");

        // emptyMap is empty map

        integerMap.put(1, 123);
        integerMap.put(2, 234);
        integerMap.put(3, 345);

        atomicIntegerMap.put(1, new AtomicInteger(1123));
        atomicIntegerMap.put(2, new AtomicInteger(1234));
        atomicIntegerMap.put(3, new AtomicInteger(1345));

        enumIntegerMap.put(Key.FIRST, 123);
        enumIntegerMap.put(Key.SECOND, 234);
        enumIntegerMap.put(Key.THIRD, 345);
    }


    public static void main(String[] args) {
        JmxMapTestApplication test = new JmxMapTestApplication();
        new Scanner(System.in).nextLine();
        System.out.println("Added record to empty map");
        test.emptyMap.put(123, 456);

        new Scanner(System.in).nextLine();
    }
}