package com.avast.cloudutils.jmx;

import com.avast.client.jmx.JMXClientConnection;
import junit.framework.TestCase;
import org.junit.Test;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.Random;

/**
 * Created <b>18.11.13</b><br>
 *
 * @author Jenda Kolena, kolena@avast.com
 * @version 0.1
 */
public class JmxPropertyTest extends TestCase {

    /*
    THIS TEST HAS TO BE RUN WITH VM PARAMETER:
    -Dcom.sun.management.jmxremote.port=9969 -Djava.rmi.server.hostname=localhost -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false

    The port number can be changed, but it has to be the same as in the connection below.
     */


    @Test
    public void testReadableAndSetable() throws IOException {
        new JmxTestApplication();

        JMXClientConnection connection;
        try {
            connection = new JMXClientConnection("localhost:9969");
        } catch (IOException e) {
            //ignore, test run with wrong parameters...
            System.err.println("Cannot connect to the service, maybe wrong parameters?");
            return;
        }
        ObjectName objectName = connection.getObjectName("com.avast.cloudutils.jmx:type=JmxTestApplication");

        /* basic */

        int valueInt = new Random().nextInt();
        connection.setAttribute(objectName, "setableInt", valueInt);
        connection.setAttribute(objectName, "setableInteger", valueInt);

        assertEquals(valueInt, connection.getAttribute(objectName, "setableInt"));
        assertEquals(valueInt, connection.getAttribute(objectName, "setableInteger"));

        long valueLong = new Random().nextLong();
        connection.setAttribute(objectName, "setableL", valueLong);
        connection.setAttribute(objectName, "setableLong", valueLong);

        assertEquals(valueLong, connection.getAttribute(objectName, "setableL"));
        assertEquals(valueLong, connection.getAttribute(objectName, "setableLong"));

        boolean valueBool = new Random().nextBoolean();
        connection.setAttribute(objectName, "setableBool", valueBool);
        connection.setAttribute(objectName, "setableBoolean", valueBool);

        assertEquals(valueBool, connection.getAttribute(objectName, "setableBool"));
        assertEquals(valueBool, connection.getAttribute(objectName, "setableBoolean"));

        String valueString = System.currentTimeMillis() + "";
        connection.setAttribute(objectName, "setableString", valueString);
        assertEquals(valueString, connection.getAttribute(objectName, "setableString"));

        /* atomic */

        connection.setAttribute(objectName, "setableAtomicInteger", valueInt);
        assertEquals(valueInt, connection.getAttribute(objectName, "setableAtomicInteger"));

        connection.setAttribute(objectName, "setableAtomicLong", valueLong);
        assertEquals(valueLong, connection.getAttribute(objectName, "setableAtomicLong"));

        connection.setAttribute(objectName, "setableAtomicBoolean", valueBool);
        assertEquals(valueBool, connection.getAttribute(objectName, "setableAtomicBoolean"));

        //not settable
        try {
            connection.getAttribute(objectName, "atomicString");
            assertTrue("Should not be here", false);

        } catch (IOException e) {
            // ok expecting the exception
        }
    }

    @Test
    public void testMapsField() throws IOException {
        JmxMapTestApplication testClass = new JmxMapTestApplication();
        String beanName = "com.avast.cloudutils.jmx:type=" + testClass.getClass().getSimpleName();

        JMXClientConnection connection;
        try {
            connection = new JMXClientConnection("localhost:9969");
        } catch (IOException e) {
            //ignore, test run with wrong parameters...
            System.err.println("Cannot connect to the service, maybe wrong parameters?");
            return;
        }
        ObjectName objectName = connection.getObjectName(beanName);


        Object obj;
        CompositeData compositeData;

        obj = connection.getAttribute(objectName, "stringMap");
        assertTrue(obj instanceof CompositeData);

        compositeData = (CompositeData) obj;
        assertTrue(compositeData.containsKey("str1"));
        assertTrue(compositeData.containsKey("str2"));


        obj = connection.getAttribute(objectName, "emptyMap");
        assertTrue(obj == null);//shouldn't be exposed

        obj = connection.getAttribute(objectName, "integerMap");
        assertTrue(obj instanceof CompositeData);
        compositeData = (CompositeData) obj;
        assertTrue(compositeData.containsKey("1"));
        assertTrue(compositeData.containsKey("2"));
        assertTrue(compositeData.containsKey("3"));

    }


    @Test
    public void testMapsFieldInAnnotatedMethod() throws IOException {
        JmxMapMethod testClass = new JmxMapMethod();
        String beanName = "com.avast.cloudutils.jmx:type=" + testClass.getClass().getSimpleName();

        JMXClientConnection connection;
        try {
            connection = new JMXClientConnection("localhost:9969");
        } catch (IOException e) {
            //ignore, test run with wrong parameters...
            System.err.println("Cannot connect to the service, maybe wrong parameters?");
            return;
        }
        ObjectName objectName = connection.getObjectName(beanName);


        Object obj;
        CompositeData compositeData;

        obj = connection.getAttribute(objectName, "stringMap");
        assertTrue(obj instanceof CompositeData);

        compositeData = (CompositeData) obj;
        assertTrue(compositeData.containsKey("str1"));
        assertTrue(compositeData.containsKey("str2"));



        obj = connection.getAttribute(objectName, "annotatedMethodMap");
        assertTrue(obj instanceof CompositeData);
        compositeData = (CompositeData) obj;
        assertTrue(compositeData.containsKey("key1"));
        assertTrue(compositeData.containsKey("key2"));

    }


    @Test
    public void testAnnotationMethod() throws Exception {
        JmxSubTestApplication testClass = new JmxSubTestApplication();
        String beanName = "com.avast.cloudutils.jmx:type=" + testClass.getClass().getSimpleName();
        String hostAndPort = "localhost:9969";

        MBeanServerConnection conn;
        JMXConnector connector = null;
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + hostAndPort + "/jmxrmi");

        try {
            conn = (connector = JMXConnectorFactory.connect(url)).getMBeanServerConnection();
        } catch (IOException var4) {
            if(connector != null) {
                connector.close();
            }

            System.err.println("Cannot connect to the service, maybe wrong parameters?");
            return;
        }

        ObjectName objectName = (ObjectName)conn.queryNames(new ObjectName(beanName), null).toArray()[0];

        assertEquals("hello", conn.getAttribute(objectName, "backedProperty"));

        assertEquals(1, conn.getAttributes(objectName, new String[]{"backedProperty"}).size());

    }
}
