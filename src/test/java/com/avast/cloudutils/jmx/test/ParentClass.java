package com.avast.cloudutils.jmx.test;

import com.avast.jmx.JMXProperty;
import com.avast.jmx.MyDynamicBean;

/**
 * Created by jacob on 6/25/15.
 */
public class ParentClass {

    @JMXProperty
    private int propertyA = 0;

    public ParentClass() {
        MyDynamicBean.exposeAndRegisterSilently(this);
    }

    @JMXProperty (name = "propertyB")
    public String getValue() {
        return "this is property B";
    }
}
