package com.avast.jmx;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanInfo;
import javax.management.ObjectName;
import java.io.Closeable;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * Created <b>29.10.2014</b><br>
 *
 * @author Jenda Kolena, kolena@avast.com
 */
@SuppressWarnings("unused")
public class JMXClientAsyncConnection implements Closeable {
    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    protected final JMXClientConnection clientConnection;

    protected final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(new ThreadFactoryBuilder()
            .setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    LOG.error("Uncaught exception in SMS processing (thread " + t.getName() + ")", e);
                }
            })
            .setNameFormat("jmxclientasyncconnection-" + System.currentTimeMillis() + "-%d").setDaemon(true).build()));

    public JMXClientAsyncConnection(String hostAndPort) throws IOException {
        try {
            clientConnection = new JMXClientConnection(hostAndPort);
        } catch (IOException e) {
            if (executor != null) {
                executor.shutdownNow();
            }
            throw e;
        }
    }

    public JMXClientAsyncConnection(String host, int port) throws IOException {
        this(host + ":" + port);
    }

    public ListenableFuture<Set<ObjectName>> getObjectNames(final ObjectName query) {
        return executor.submit(new Callable<Set<ObjectName>>() {
            @Override
            public Set<ObjectName> call() throws Exception {
                return clientConnection.getObjectNames(query);
            }
        });
    }

    public ListenableFuture<ObjectName> getObjectName(final String query) throws IOException {
        return executor.submit(new Callable<ObjectName>() {
            @Override
            public ObjectName call() throws Exception {
                return clientConnection.getObjectName(query);
            }
        });
    }

    public ListenableFuture<Boolean> isObjectName(final String query) throws IOException {
        return executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return clientConnection.isObjectName(query);
            }
        });
    }

    public ListenableFuture<Object> invoke(final ObjectName object, final String methodName, final Object[] args, final String[] types) throws IOException {
        return executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return clientConnection.invoke(object, methodName, args, types);
            }
        });
    }

    public ListenableFuture<Object> invokeSmart(final ObjectName object, final String methodName, final Object... args) throws IOException {
        return executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return clientConnection.invokeSmart(object, methodName, args);
            }
        });
    }

    public ListenableFuture<Object> invoke(final ObjectName object, final String methodName) throws IOException {
        return executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return clientConnection.invoke(object, methodName);
            }
        });
    }

    public ListenableFuture<Object> getAttribute(final ObjectName object, final String attributeName) throws IOException {
        return executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return clientConnection.getAttribute(object, attributeName);
            }
        });
    }

    public ListenableFuture<?> setAttribute(final ObjectName object, final String attributeName, final Object value) throws IOException {
        return executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                clientConnection.setAttribute(object, attributeName, value);
                return value;
            }
        });
    }

    public ListenableFuture<MBeanInfo> getBeanInfo(final ObjectName object) throws IOException {
        return executor.submit(new Callable<MBeanInfo>() {
            @Override
            public MBeanInfo call() throws Exception {
                return clientConnection.getBeanInfo(object);
            }
        });
    }

    public void close() throws IOException {
        executor.shutdownNow();
        clientConnection.close();
    }
}
