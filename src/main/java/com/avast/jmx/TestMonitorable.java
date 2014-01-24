/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.avast.jmx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Tomas Rehak <rehak@avast.com>
 */
@SuppressWarnings("unused")
public class TestMonitorable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestMonitorable.class);
    @JMXProperty(setable = true)
    private long primitiveCnt = 0L;
    @JMXProperty
    private AtomicInteger atomicCnt = new AtomicInteger(0);
    @JMXProperty
    private AtomicInteger atomicCntSetable = new AtomicInteger(0);
    @JMXProperty
    private Map<String, Long> mapStats = new HashMap<String, Long>();

    public static void main(String[] args) throws Exception {
        TestMonitorable tm = new TestMonitorable();
        tm.testMonitorable();
    }

    @JMXProperty
    public long defaultGetMethodProperty() {
        return 324324L;
    }

    @JMXProperty(name = "renamedGetMethodProperty")
    public long defaultGetMethodProperty2() {
        return 0xabcdef;
    }

    @JMXProperty
    public long wtf() {
        return 032L;
    }


    @JMXPropertySetter
    public void setPrimitiveCnt(long num) {
        LOGGER.debug("Set method invoked");
        primitiveCnt = num;
    }

    @JMXPropertyGetter
    public long getPrimitiveCnt() {
        LOGGER.debug("Get method invoked");
        return primitiveCnt;
    }

    @JMXOperation
    public void jmxOperationTest(String param) {
        System.out.println("This is @JMXOperation, called with parameter " + param);
    }

    public void testMonitorable() throws Exception {
        MyDynamicBean mdb = new MyDynamicBean(this);
        mdb.register();
        Thread.sleep(Long.MAX_VALUE);
    }
}
