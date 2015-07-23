package com.avast.cloudutils.jmx;

import com.avast.jmx.JMXProperty;
import com.avast.jmx.MyDynamicBean;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by jacob on 7/21/15.
 */
public class JmxMapMethod {

    @JMXProperty
    public Map<String, String> stringMap = new HashMap<>();


    @JMXProperty(name = "annotatedMethodMap")
    public Map<String, Integer> getMapAsProperty() {
        Map<String, Integer> r = new HashMap<>();
        r.put("key1", 1);
        r.put("key2", 2);
        return r;
    }


    public JmxMapMethod() {
        MyDynamicBean.exposeAndRegisterSilently(this);
        stringMap.put("str1", "strVa1");
        stringMap.put("str2", "strVa2");
    }

    public static void main(String[] args) {
        JmxMapMethod test = new JmxMapMethod();
        new Scanner(System.in).nextLine();
    }
}
