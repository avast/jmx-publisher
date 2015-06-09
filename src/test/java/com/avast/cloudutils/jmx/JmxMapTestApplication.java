package com.avast.cloudutils.jmx;

import com.avast.jmx.JMXProperty;
import com.avast.jmx.JMXPropertyGetter;
import com.avast.jmx.JMXPropertySetter;
import com.avast.jmx.MyDynamicBean;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by barina on 08/06/15.
 */
public class JmxMapTestApplication {

    @JMXProperty
    public Map<String, String> stringMap = new HashMap<>();

    @JMXProperty
    public Map<Integer, Integer> emptyMap = new HashMap<>();

    @JMXProperty
    public Map<Integer, String> integerStringMap = new HashMap<>();

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

        integerStringMap.put(1, "int1");
        integerStringMap.put(2, "int2");
        integerStringMap.put(3, "int3");
    }


    public static void main(String[] args) {
        JmxMapTestApplication test = new JmxMapTestApplication();
        new Scanner(System.in).nextLine();
    }
}