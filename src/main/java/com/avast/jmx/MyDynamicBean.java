package com.avast.jmx;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Jan Kolena (originally: Tomas Rehak <rehak@avast.com>)
 */
public final class MyDynamicBean implements DynamicMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyDynamicBean.class);
    public static final String IS_PREFIX = "is";
    public static final String GET_PREFIX = "get";
    public static final String SET_PREFIX = "set";
    private final String name;
    private final Object obj;
    private final MBeanInfo info;
    private final Map<String, Map<String, Method>> ops = new ConcurrentHashMap<String, Map<String, Method>>();
    private final Map<String, Property> props = new ConcurrentHashMap<String, Property>();
    private static final Map<String, AtomicLong> names = new ConcurrentHashMap<String, AtomicLong>();
    private static final ReentrantLock lock = new ReentrantLock();

    /**
     * Exposes and register MBean (one-line method)
     *
     * @param obj object to expose
     * @return MyDynamicBean (to be able to call unregister), NULL on exception
     */
    public static MyDynamicBean exposeAndRegisterSilently(Object obj) {
        try {
            MyDynamicBean mdb = new MyDynamicBean(obj);
            mdb.register();
            return mdb;
        } catch (Exception e) {
            LOGGER.error("Exception while registering JMX Bean", e);
            return null;
        }
    }

    /**
     * Exposes and register MBean (one-line method)
     *
     * @param name object name
     * @param obj  object to expose
     * @return MyDynamicBean (to be able to call unregister), NULL on exception
     */
    public static MyDynamicBean exposeAndRegisterSilently(String name, Object obj) {
        try {
            MyDynamicBean mdb = new MyDynamicBean(name, obj);
            mdb.register();
            return mdb;
        } catch (Exception e) {
            LOGGER.error("Exception while registering JMX Bean", e);
            return null;
        }
    }

    private ObjectName objName;

    public MyDynamicBean(Object object) {
        this(null, "Default description", object);
    }

    public MyDynamicBean(String name, Object object) {
        this(name, "Default description", object);
    }

    public MyDynamicBean(String name, String description, Object object) {
        Preconditions.checkNotNull(object);
        if (description == null) {
            description = "Default description";
        }
        if (name == null) {
            name = getJMXNameForClass(object.getClass());
        }
        try {
            this.name = name;
            this.obj = object;
            Constructor[] constructors = MyDynamicBean.class.getConstructors();
            MBeanConstructorInfo[] constr = new MBeanConstructorInfo[1];
            constr[0] = new MBeanConstructorInfo(getClass().getSimpleName() + "(): No-parameter constructor", constructors[0]);
            // new
            // get all methods annotated as JMXPropertyGetter
            List<Method> getters = MyDynamicBean.getAnnotatedMethods(obj.getClass(), JMXPropertyGetter.class);
            // get all methods annotated as JMXPropertySetter
            List<Method> setters = MyDynamicBean.getAnnotatedMethods(obj.getClass(), JMXPropertySetter.class);
            // get all fields annotated as JMXProperty
            List<Field> fields = MyDynamicBean.getAnnotatedFields(obj.getClass(), JMXProperty.class);
            // get all methods annotated as JMXProperty (getters-only, gives value)
            List<Method> propertyMethods = MyDynamicBean.getAnnotatedMethods(obj.getClass(), JMXProperty.class);
            // get all methods annotated as JMXOperation
            List<Method> operations = MyDynamicBean.getAnnotatedMethods(obj.getClass(), JMXOperation.class);
            // process operations, store to this.ops
            assignOperations(operations);
            // create operations info for MBeanInfo
            MBeanOperationInfo[] operationsInfo = MyDynamicBean.createOperationsInfo(operations);
            // create attribute info for MBeanInfo
            List<Property> properties = MyDynamicBean.getProperties(obj, fields, getters, setters, propertyMethods);
            properties = MyDynamicBean.generateMissingGetters(properties);
            properties = MyDynamicBean.generateMissingSetters(properties);
            for (Property p : properties) {
                if (props.get(p.getName()) != null) {
                    throw new IllegalArgumentException("Duplicate property " + p.getName() + " " + p);
                }
                props.put(p.getName(), p);
            }
            MBeanAttributeInfo[] attributes = MyDynamicBean.propertiesToAttributeInfo(properties);
            this.info = new MBeanInfo(name, description, attributes, constr, operationsInfo, new MBeanNotificationInfo[0]);

        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static MBeanAttributeInfo[] propertiesToAttributeInfo(List<Property> properties) {
        MBeanAttributeInfo[] infos = new MBeanAttributeInfo[properties.size()];
        for (int i = 0; i < properties.size(); i++) {
            infos[i] = MyDynamicBean.propertyToAttribute(properties.get(i));
        }
        return infos;
    }

    public static List<Property> generateMissingGetters(List<Property> properties) throws NoSuchMethodException {
        Preconditions.checkNotNull(properties);
        for (Property p : properties) {
            boolean readable = p.isReadable();
            boolean setable = p.isSetable();
            Method getter = p.getGetter();
            if (!setable && !readable) {
                throw new IllegalArgumentException("Property " + p + " is not settable, nor readable - makes no sense!");
            }
            // getters
            // ==============
            if (readable) {
                if (getter == null) {
                    Getter get = createGetter(p.getField(), p.getInstance());
                    getter = get.getClass().getMethod("get");
                    p.setGetterTarget(get);
                }
            }
            else {
                getter = null;
            }
            p.setGetter(getter);
        }
        return properties;
    }

    public static List<Property> generateMissingSetters(List<Property> properties) throws NoSuchMethodException {
        for (Property p : properties) {
            boolean readable = p.isReadable();
            boolean setable = p.isSetable();
            Method setter = p.getSetter();
            if (!setable && !readable) {
                throw new IllegalArgumentException("Property " + p + " is not settable, nor readable - makes no sense!");
            }
            // setters
            // ==============
            if (setable) {
                if (setter == null) {
                    Setter set = createSetter(p.getField(), p.getInstance());
                    setter = set.getClass().getMethod("set", Object.class);
                    p.setSetterTarget(set);

                    String pType = p.getType();
                    if (pType.substring(pType.lastIndexOf(".") + 1).startsWith("Atomic")) {
                        p.setType(pType.replace("java.util.concurrent.atomic.Atomic", "java.lang."));
                    }

                }
            }
            else {
                setter = null;
            }
            p.setSetter(setter);
        }
        return properties;
    }

    public static List<Property> getProperties(Object instance, List<Field> fields, List<Method> gets, List<Method> sets, List<Method> propertyMethods) {
        List<Property> list = new ArrayList<Property>();
        Map<String, Field> fm = new HashMap<String, Field>();
        Map<String, Method> getters = new HashMap<String, Method>();
        Map<String, Method> setters = new HashMap<String, Method>();
        // map fields
        for (Field f : fields) {
            JMXProperty an = f.getAnnotation(JMXProperty.class);
            Preconditions.checkNotNull(an);
            String name;
            if (an.name() == null || an.name().trim().equals("")) {
                name = f.getName();
            }
            else {
                name = an.name();
            }
            if (fm.get(name) != null) {
                throw new IllegalArgumentException("Duplicate attribute name " + f);
            }
            fm.put(name, f);
        }
        // map getters
        for (Method m : gets) {
            JMXPropertyGetter an = m.getAnnotation(JMXPropertyGetter.class);
            Preconditions.checkNotNull(an);
            String name;
            if (an.name() == null || an.name().trim().equals("")) {
                name = m.getName();
                name = stripGetterPrefix(name);
            }
            else {
                name = an.name();
            }
            if (getters.get(name) != null) {
                throw new IllegalArgumentException("Duplicate getter name " + m);
            }
            getters.put(name, m);
        }
        // map setters
        for (Method m : sets) {
            JMXPropertySetter an = m.getAnnotation(JMXPropertySetter.class);
            Preconditions.checkNotNull(an);
            String name;
            if (an.name() == null || an.name().trim().equals("")) {
                name = m.getName();
                name = stripSetterPrefix(name);
            }
            else {
                name = an.name();
            }
            if (setters.get(name) != null) {
                throw new IllegalArgumentException("Duplicate setter name " + m);
            }
            setters.put(name, m);
        }
        // assign properties from fields
        for (String n : fm.keySet()) {
            Field f = fm.get(n);
            JMXProperty anot = f.getAnnotation(JMXProperty.class);
            Method setter = setters.get(n);
            Method getter = getters.get(n);
            Property prop = new Property(instance, f, n, anot.description(), anot.readeable(), anot.setable(), getter, setter);
            prop.setType(f.getType().getName());
            list.add(prop);
        }
        // assign property methods
        for (Method m : propertyMethods) {
            JMXProperty an = m.getAnnotation(JMXProperty.class);
            Preconditions.checkNotNull(an);
            String name;
            if (an.name() == null || an.name().trim().equals("")) {
                name = m.getName();
            }
            else {
                name = an.name();
            }
            if (setters.get(name) != null) {
                throw new IllegalArgumentException("Duplicate getter name " + m);
            }
            if (getters.get(name) != null) {
                throw new IllegalArgumentException("Duplicate getter name " + m);
            }
            if (fm.get(name) != null) {
                throw new IllegalArgumentException("Duplicate getter name " + m);
            }
            Property prop = new Property(instance, null, name, an.description(), true, false, m, null);
            prop.setType(m.getReturnType().getName());
            list.add(prop);
        }
        // lets validate that there are no orphaned setters / getters
        Set<String> settersNames = setters.keySet();
        Set<String> gettersNames = getters.keySet();
        Set<String> propNames = new HashSet<String>();
        for (Property prop : list) {
            String propName = prop.getName();
            propNames.add(propName);
        }
        // remove attribute names from setter names
        settersNames.removeAll(propNames);
        // should be empty set
        if (!settersNames.isEmpty()) {
            throw new IllegalArgumentException("Orphaned setters : " + settersNames + ", not matching fields!");
        }
        // remove attribute names from getter names
        gettersNames.removeAll(propNames);
        // should be empty set
        if (!gettersNames.isEmpty()) {
            throw new IllegalArgumentException("Orphaned getters : " + gettersNames + ", not matching fields!");
        }
        return list;
    }

    public static String stripGetterPrefix(String name) {
        Preconditions.checkNotNull(name);
        return stripMethodPrefix(name, new String[]{IS_PREFIX, GET_PREFIX});
    }

    public static String stripMethodPrefix(String name, String[] prefixes) {
        Preconditions.checkNotNull(name);
        String res = name;
        for (String prefix : prefixes) {
            if (name.startsWith(prefix) && name.length() > prefix.length()) {
                res = name.substring(prefix.length());
                char[] chars = res.toCharArray();
                chars[0] = Character.toLowerCase(chars[0]);
                res = new String(chars);
                break;
            }
        }
        LOGGER.debug("Striping prefix from method " + name + " -> " + res);
        return res;
    }

    public static String stripSetterPrefix(String name) {
        Preconditions.checkNotNull(name);
        return stripMethodPrefix(name, new String[]{SET_PREFIX});
    }

    private void assignOperations(List<Method> methods) {
        for (Method m : methods) {
            JMXOperation opAnot = m.getAnnotation(JMXOperation.class);
            Preconditions.checkNotNull(opAnot);
            String opName = m.getName();
            Class<?>[] parameterTypes = m.getParameterTypes();
            String signature = methodParametersToSignature(parameterTypes);
            Map<String, Method> get = ops.get(opName);
            if (get == null) {
                get = new ConcurrentHashMap<String, Method>();
                ops.put(opName, get);
            }
            get.put(signature, m);
        }
    }

    public static String getJMXNameForClass(Class<?> cls) {
        lock.lock();
        try {
            String packageName = cls.getPackage() == null ? "default" : cls.getPackage().getName();
            String name = packageName + ":type=" + cls.getSimpleName();
            AtomicLong get = MyDynamicBean.names.get(name);
            if (get != null) {
                name += "#" + get.incrementAndGet();
            }
            else {
                get = new AtomicLong(0);
                names.put(name, get);
            }
            return name;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        Preconditions.checkNotNull(attribute);
        Property prop = props.get(attribute);
        if (prop != null) {
            try {
                return prop.getValue();
            } catch (Exception ex) {
                LOGGER.error("Error executing getter for " + prop, ex);
                throw new RuntimeException("Error getting value for " + prop, ex);
            }
        }
        throw new IllegalArgumentException("Field " + attribute + " not found");
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        Preconditions.checkNotNull(attribute);
        String name = attribute.getName();
        Object val = attribute.getValue();
        Property prop = props.get(name);
        if (prop != null) {
            try {
                prop.setValue(val);
            } catch (Exception ex) {
                LOGGER.error("Error executing setter for " + prop, ex);
                throw new RuntimeException("Error setting value for " + prop, ex);
            }
        }
        else {
            throw new IllegalArgumentException("Field " + attribute + " not found");
        }
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        LOGGER.debug("Get attributes " + Arrays.asList(attributes));
        AttributeList list = new AttributeList();
        for (String attr : attributes) {
            Property p = props.get(attr);
            if (p != null) {
                try {
                    list.add(new Attribute(p.getName(), p.getValue()));
                } catch (Exception ex) {
                    LOGGER.error("Exception while creating attribute", ex);
                    throw new IllegalArgumentException("Error encountered", ex);
                }
            }
            else {
                throw new IllegalArgumentException("Unknown attribute " + attr);
            }
        }
        return list;
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        Preconditions.checkNotNull(attributes);
        String[] attribs = new String[attributes.size()];
        for (int i = 0; i < attributes.size(); i++) {
            Attribute atr = (Attribute) attributes.get(i);
            Preconditions.checkNotNull(atr);
            Property prop = props.get(atr.getName());
            try {
                prop.setValue(atr.getValue());
            } catch (Exception ex) {
                LOGGER.error("Exception while setting attribute", ex);
                throw new IllegalArgumentException("Error encountered", ex);
            }
            attribs[i] = atr.getName();
        }
        return getAttributes(attribs);
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        LOGGER.debug("Invoking " + actionName + ", params " + Arrays.toString(params) + ", signature: " + Arrays.toString(signature));
//        System.out.println("Invoking " + actionName + ", params " + Arrays.toString(params) + ", signature: " + Arrays.toString(signature));
        Map<String, Method> get = ops.get(actionName);
        if (get != null) {
            String sig = "";
            for (int i = 0; i < signature.length; i++) {
                sig += signature[i];
                if (i < signature.length - 1) {
                    sig += ",";
                }
            }
            Method m = get.get(sig);
            if (m != null) {
                try {
                    return m.invoke(obj, params);
                } catch (Exception ex) {
                    throw new MBeanException(ex, "Error invoking operation");
                }
            }
            else {
                throw new IllegalArgumentException("Operation " + actionName + " not found!");
            }
        }
        else {
            throw new IllegalArgumentException("Operation " + actionName + " not found!");
        }
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        LOGGER.debug("Get MBean info " + info);
        return info;
    }

    private static MBeanOperationInfo[] createOperationsInfo(List<Method> operations) {
        Preconditions.checkNotNull(operations);
        List<MBeanOperationInfo> list = new ArrayList<MBeanOperationInfo>();
        for (Method m : operations) {
            JMXOperation anot = m.getAnnotation(JMXOperation.class);
            Preconditions.checkNotNull(anot);
            MBeanOperationInfo inf = new MBeanOperationInfo(anot.description(), m);
            list.add(inf);
        }
        return list.toArray(new MBeanOperationInfo[list.size()]);
    }

    private static String methodParametersToSignature(Class<?>[] parameterTypes) {
        String signature = "";
        for (int i = 0; i < parameterTypes.length; i++) {
            signature += parameterTypes[i].getName();
            if (i < parameterTypes.length - 1) {
                signature += ",";
            }
        }
        return signature;
    }

    private static Setter createSetter(Field f, Object obj) throws NoSuchMethodException {
        f.setAccessible(true);
        return Setter.newSetter(obj, f);
    }

    private static Getter createGetter(Field f, Object obj) throws NoSuchMethodException {
        f.setAccessible(true);
        return Getter.newGetter(obj, f);
    }

//    public static String getFieldPropName(Field f) {
//        JMXProperty fa = f.getAnnotation(JMXProperty.class);
//        Preconditions.checkArgument(fa != null, "Field " + f + " is not Monitorable");
//        String propName;
//        // property opName is either one supplied in parameter
//        assert fa != null;
//        if (!fa.name().isEmpty()) {
//            propName = fa.name();
//        }
//        else {
//            // or field opName, if not changed
//            propName = f.getName();
//        }
//        return propName;
//    }

    private static List<Method> getAnnotatedMethods(Class<?> objectClass, Class annotationClass) {
        List<Method> list = new ArrayList<Method>();
        Method[] methods = objectClass.getMethods();
        for (Method m : methods) {
            Annotation an = m.getAnnotation(annotationClass);
            if (an != null) {
                list.add(m);
            }
        }
        return list;
    }

//    public static Method getSetterForField(Field f, Class cls) {
//        String propName = getFieldPropName(f);
//        // find corresponding method
//        List<Method> methods = getAnnotatedMethods(cls, JMXPropertySetter.class);
//        for (Method m : methods) {
//            JMXPropertySetter ma = m.getAnnotation(JMXPropertySetter.class);
//            if (ma.name().equals(propName)) {
//                return m;
//            }
//        }
//        // not found
//        return null;
//    }

//    public static Method getGetterForField(Field f, Class cls) {
//        String propName = getFieldPropName(f);
//        // find corresponding method
//        List<Method> methods = getAnnotatedMethods(cls, JMXPropertyGetter.class);
//        for (Method m : methods) {
//            JMXPropertyGetter ma = m.getAnnotation(JMXPropertyGetter.class);
//            if (ma.name().equals(propName)) {
//                return m;
//            }
//        }
//        // not found
//        return null;
//    }

    public static MBeanAttributeInfo propertyToAttribute(Property prop) {
        LOGGER.debug("Processing property " + prop);
        Preconditions.checkNotNull(prop);
        boolean readable = prop.isReadable();
        boolean setable = prop.isSetable();
        Preconditions.checkArgument(readable || setable, "Property is not setable, nor readable! " + prop);
        return new MBeanAttributeInfo(prop.getName(), prop.getType(), prop.getDesc(), prop.isReadable(), prop.isSetable(), false);
    }

    public static List<Field> getAnnotatedFields(Class<?> toMonitor, Class<? extends Annotation> annotationClass) {
        List<Field> fields = new ArrayList<Field>();
        Class<?> cls = toMonitor;
        while (cls != null) {
            Field[] declaredFields = cls.getDeclaredFields();
            for (Field f : declaredFields) {
                // is f JMXProperty?
                if (f.getAnnotation(annotationClass) != null) {
                    fields.add(f);
                }
            }
            cls = cls.getSuperclass();
        }
        return fields;
    }

    @SuppressWarnings("unused")
    public static boolean isSetable(Field f) {
        JMXProperty anot = f.getAnnotation(JMXProperty.class);
        if (anot != null) {
            return anot.setable();
        }
        throw new IllegalArgumentException("Field " + f + " is not Monitorable");
    }

    @SuppressWarnings("unused")
    public void unregister() throws InstanceNotFoundException, MBeanRegistrationException {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        mbs.unregisterMBean(objName);
    }

    public void register() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            this.objName = new ObjectName(name);
            mbs.registerMBean(this, objName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
