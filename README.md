Title: Apache Isis Publishing Service using ActiveMQ RA

An implementation of an Apache Isis org.apache.isis.applib.services.publish.PublishingService that publishes to ActiveMQ using the Resource Adapter and XA/JTA.

> This is a work-in-progress.  To date, have figured out the lookup of JNDI connections from TomEE, but haven't yet got a full handle on the commit/rollback scenarios.

### Configuration

The service looks up this connection from JNDI using values taken from `isis.properties` configuration file.

The following are used to locate JNDI itself (through the call to `InitialContext()`).  If missing, then the default JNDI within the JEE container is used:
<pre>
com.danhaywood.isis.publishingservice.activemq.ra.java.naming.factory.initial=...
com.danhaywood.isis.publishingservice.activemq.ra.java.naming.provider.url=...
</pre>

The following are used to locate the connectionFactory and queue within JNDI.  Fir example:
<pre>
com.danhaywood.isis.publishingservice.activemq.ra.connectionFactory=...
com.danhaywood.isis.publishingservice.activemq.ra.queue=...
</pre>

The default value for the connection factory is simply `ConnectionFactory`.  The default value for the queue is simply `queue`.


#### TomEE Plus Configuration

If using TomEE Plus (tested against 1.5.1), then declare the connection to ActiveMQ in `CATALINA_HOME/conf/tomee.xml`:

<pre>
&lt;tomee&gt;

  ...

  &lt;Resource id=&quot;JmsResourceAdapter&quot; type=&quot;ActiveMQResourceAdapter&quot;&gt;
    BrokerXmlConfig = 
    ServerUrl = tcp://localhost:61616
  &lt;/Resource&gt;

  &lt;Connector id=&quot;ConnectionFactory&quot; type=&quot;javax.jms.ConnectionFactory&quot;&gt;
    ResourceAdapter               = JmsResourceAdapter
    TransactionSupport            = xa
    PoolMaxSize                   = 150
    PoolMinSize                   = 0
    ConnectionMaxWaitMilliseconds = 15000
    ConnectionMaxIdleMinutes      = 15
  &lt;/Connector&gt;

  ...

&lt;/tomee&gt;
</pre>

The above assumes that ActiveMQ is running on `localhost`; adjust if necessary.

In the `isis.properties` configuration file, define the connection factory as `openejb:Resource/ConnectionFactory`:

<pre>
com.danhaywood.isis.publishingservice.activemq.ra.connectionFactory=openejb:Resource/ConnectionFactory
</pre>


