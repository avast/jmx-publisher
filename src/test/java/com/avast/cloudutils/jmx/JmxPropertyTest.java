package com.avast.cloudutils.jmx;

import com.avast.client.jmx.JMXClientConnection;
import junit.framework.TestCase;
import org.junit.Test;

import javax.management.ObjectName;
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
        assertEquals("helloWorld", connection.getAttribute(objectName, "atomicString"));
    }
}
