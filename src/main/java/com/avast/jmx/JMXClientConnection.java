package com.avast.jmx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Set;

/**
 * Created <b>22.10.13</b><br>
 *
 * @author Jenda Kolena, kolena@avast.com
 * @version 0.1
 */
@SuppressWarnings("unused")
public class JMXClientConnection {
    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    public static final String JAVA_DOUBLE_TYPE = Double.TYPE.getName();
    public static final String JAVA_DOUBLE = Double.class.getName();
    public static final String JAVA_INTEGER_TYPE = Integer.TYPE.getName();
    public static final String JAVA_INTEGER = Integer.class.getName();
    public static final String JAVA_STRING = String.class.getName();
    public static final String JAVA_CHAR_TYPE = Character.TYPE.getName();
    public static final String JAVA_CHAR = Character.class.getName();
    public static final String JAVA_FLOAT_TYPE = Float.TYPE.getName();
    public static final String JAVA_FLOAT = Float.class.getName();
    public static final String JAVA_BOOLEAN_TYPE = Boolean.TYPE.getName();
    public static final String JAVA_BOOLEAN = Boolean.class.getName();

    private JMXConnector connector;
    private MBeanServerConnection conn;

    public JMXClientConnection(String hostAndPort) throws IOException, MalformedURLException {
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + hostAndPort + "/jmxrmi");
        conn = (connector = JMXConnectorFactory.connect(url)).getMBeanServerConnection();
    }

    public JMXClientConnection(String host, int port) throws IOException {
        this(host + ":" + port);
    }

    public Set<ObjectName> getObjectNames(ObjectName query) throws IOException {
        return conn.queryNames(query, null);
    }

    public ObjectName getObjectName(String query) throws IOException {
        try {
            return (ObjectName) getObjectNames(new ObjectName(query)).toArray()[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;//empty array, not exist
        } catch (MalformedObjectNameException e) {
            throw new IOException(e);
        }
    }

    public boolean isObjectName(String query) throws IOException {
        return getObjectName(query) != null;
    }

    public Object invoke(ObjectName object, String methodName, Object[] args, String[] types) throws IOException {
        try {
            return conn.invoke(object, methodName, args, types);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public Object invokeSmart(ObjectName object, String methodName, Object... args) throws IOException {
        String[] types = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i].getClass().getName();
        }

        return invoke(object, methodName, args, types);
    }

    public Object invoke(ObjectName object, String methodName) throws IOException {
        return invoke(object, methodName, new Object[0], new String[0]);
    }

    public Object getAttribute(ObjectName object, String attributeName) throws IOException {
        try {
            return conn.getAttribute(object, attributeName);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void setAttribute(ObjectName object, String attributeName, Object value) throws IOException {
        try {
            conn.setAttribute(object, new Attribute(attributeName, value));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public MBeanInfo getBeanInfo(ObjectName object) throws IOException {
        try {
            return conn.getMBeanInfo(object);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void close() throws IOException {
        connector.close();
    }
}
