JMX publisher
=============

Tool to get properties and methods published via JMX easily.

It consist of following annotations:

  * @JMXProperty - annotates field OR method that should be visible through JMX. Method should have no parameters, it's result is returned.
  * @JMXPropertyGetter - annotates method that serves as getter for specific field
  * @JMXPropertySetter - annotates method that serves as setter for specific field
  * @JMXOperation - annotates method (must be public) that servers as JMX operation

Usage is fairly simple - user annotates fields or method that he wants to show via JMX as @JMXProperty. As long as these fields are java types (no custom types), everything should work out of the box.
There is no need to create getters for fields, methods marked as @JMXPropertyGetter are optional (however, return type must be compatible with property type).
By the same logic, you can mark method as a @JMXProperty and its result will be published as it was an ordinary property - no need to create cover property.

Setters and getters can have names - this links them to the field. Field can have names too, if no name is specified, field name is used.
If there is no name specified for setter/getter, framework tries to automatically resolve what property do these methods belong to - stripping get/is/set prefix and lowercasing first letter. See example below.

Finally, there is the MyDynamicBean class that processes annotations and exposes JMX properties.
Just like this:

    ClassWithJMXProperties obj = new ClassWithJMXProperties();
    ...
    ...
    MyDynamicBean.exposeAndRegisterSilently(obj);//expose JMX


Larger Example:

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

        @JMXProperty(name="cacheSize")
        public long getCacheSize() {
            return mapStats.size();
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
            System.out.println("This is @JMXOperation, called with parameter "+param);
        }

        public void testMonitorable() throws Exception {
            //can be called like that, or simplier: MyDynamicBean.exposeAndRegisterSilently(this);
            MyDynamicBean mdb = new MyDynamicBean(this);
            mdb.register();
            Thread.sleep(Long.MAX_VALUE);
        }

* As setable can be used properties of following types: int, long, boolean, Integer, Long, Boolean, String, AtomicInteger, AtomicLong and AtomicBoolean
* The atomic types looks like its basic class, e.g. AtomicInteger is shown like it was an ordinary Integer (see example usage below).

#JMX client

`JMXClientConnection` class provides a good way to access the JMX properties (and operations) directly from Java/Scala program.

Small example of usage (snippet from tests):

      JMXClientConnection connection = new JMXClientConnection("localhost:9969");
      ObjectName objectName = connection.getObjectName("com.avast.jmx:type=JmxTestApplication");

      int valueInt = new Random().nextInt();
      connection.setAttribute(objectName, "setableInt", valueInt);
      connection.setAttribute(objectName, "setableInteger", valueInt);

      assertEquals(valueInt, connection.getAttribute(objectName, "setableInt"));
      assertEquals(valueInt, connection.getAttribute(objectName, "setableInteger"));
or (Scala):

      val jmx = new JMXClientConnection(URL_JMX, PORT_JMX)

      val o = jmx.getObjectName("com.avast.sb.plugins:type=WinQualPlugin")

      assert(o != null)

      val oldVal = jmx.getAttribute(o, "dumpPercentage")

      jmx.invoke(o, "setDumpPercentage", Array(1.0.asInstanceOf[AnyRef]), Array(JMXClientConnection.JAVA_DOUBLE_TYPE))
Unfortunately, method invoking requires exact parameter typing (or the method would not be found by reflection).

Atomic properties usage:

      connection.setAttribute(objectName, "setableAtomicInteger", valueInt);
      assertEquals(valueInt, connection.getAttribute(objectName, "setableAtomicInteger"));

Just like and ordinary Integer...

