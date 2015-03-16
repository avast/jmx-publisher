package com.avast.cloudutils.jmx;

import com.avast.jmx.JMXProperty;
import com.avast.jmx.MyDynamicBean;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created <b>18.11.13</b><br>
 *
 * @author Jenda Kolena, kolena@avast.com
 * @version 0.1
 */
@SuppressWarnings("unused")
public class JmxTestApplication {
    @JMXProperty(setable = true)
    public int setableInt = 0;
    @JMXProperty(setable = true)
    public Integer setableInteger = 0;

    @JMXProperty(setable = true)
    public boolean setableBool = true;
    @JMXProperty(setable = true)
    public Boolean setableBoolean = true;

    @JMXProperty(setable = true)
    public long setableL = 0;
    @JMXProperty(setable = true)
    public Long setableLong = 0l;

    @JMXProperty(setable = true)
    public String setableString = "";


    @JMXProperty(setable = true)
    public AtomicInteger setableAtomicInteger = new AtomicInteger(0);
    @JMXProperty(setable = true)
    public AtomicBoolean setableAtomicBoolean = new AtomicBoolean(true);
    @JMXProperty(setable = true)
    public AtomicLong setableAtomicLong = new AtomicLong(0);

    public JmxTestApplication() {
        MyDynamicBean.exposeAndRegisterSilently(this);
    }

    public static void main(String[] args) {
        new JmxTestApplication();
        new Scanner(System.in).nextLine();

    }
}
