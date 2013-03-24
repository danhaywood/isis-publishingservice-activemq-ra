package com.danhaywood.isis.publishingservice.activemq.ra;

import java.util.Hashtable;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.isis.applib.ApplicationException;
import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.applib.annotation.Named;
import org.apache.isis.applib.annotation.Prototype;
import org.apache.isis.applib.services.publish.EventMetadata;
import org.apache.isis.applib.services.publish.EventPayload;
import org.apache.isis.applib.services.publish.EventSerializer;
import org.apache.isis.applib.services.publish.PublishingService;

public class PublishingServiceUsingActiveMqRa implements PublishingService {

    private final static Logger LOG = LoggerFactory.getLogger(PublishingServiceUsingActiveMqRa.class);
    
    private final static String NAMING_FACTORY = "com.danhaywood.isis.publishingservice.activemq.ra.java.naming.factory.initial";
    private final static String PROVIDER_URL = "com.danhaywood.isis.publishingservice.activemq.ra.java.naming.provider.url";
    
    private static final String CONNECTION_FACTORY = "com.danhaywood.isis.publishingservice.activemq.ra.connectionFactory";
    private static final String CONNECTION_FACTORY_DEFAULT = "ConnectionFactory";
    
    private final static String QUEUE = "com.danhaywood.isis.publishingservice.activemq.ra.queue";
    private final static String QUEUE_DEFAULT = "queue";

    private InitialContext jndiContext;
    
    private ConnectionFactory jmsConnectionFactory;
    private Connection jmsConnection;

    private EventSerializer eventSerializer;

    private String queue;

    private boolean transacted = true;
    
    @Hidden
    @PostConstruct
    public void init(Map<String,String> properties) {
        
            
        LOG.info("JMS connection factory not injected, so looking up from JNDI");
        String namingFactory = properties.get(NAMING_FACTORY);
        if (namingFactory == null) {
            LOG.info("Naming factory not configured, skipping");
        }
        final String providerUrl = properties.get(PROVIDER_URL);
        if (providerUrl == null) {
            LOG.info("Provider URL not configured, skipping");
        }
        String connectionFactory = properties.get(CONNECTION_FACTORY);
        if (connectionFactory == null) {
            connectionFactory = CONNECTION_FACTORY_DEFAULT;
            LOG.info("Provider URL not configured, using default");
        }
        queue = properties.get(QUEUE);
        if(queue == null) {
            queue = QUEUE_DEFAULT;
            LOG.info("Queue name not configured, using default");
        }

        LOG.info("Naming factory    : " + namingFactory);
        LOG.info("Provider URL      : " + providerUrl);
        LOG.info("Connection factory: " + connectionFactory);
        LOG.info("Queue             : " + queue);

        Hashtable<String, String> ctxProps = new Hashtable<String, String>();
        if (namingFactory != null) {
            ctxProps.put("java.naming.factory.initial", namingFactory);
        }
        if (providerUrl != null) {
            ctxProps.put("java.naming.provider.url", providerUrl);
        }

        try {
            jndiContext = new InitialContext(ctxProps);
        } catch (NamingException e) {
            LOG.error(e.getExplanation());
            return;
        }

        try {
            jmsConnectionFactory = (ConnectionFactory) jndiContext.lookup(connectionFactory);
        } catch (NamingException e) {
            LOG.error(e.getExplanation(), e);
            return;
        }

        LOG.info("Successfully looked up '" + connectionFactory + "'");

        try {
            jmsConnection = jmsConnectionFactory.createConnection();
        } catch (JMSException e) {
            LOG.error("Unable to create connection", e);
            return;
        }
        
        try {
            jmsConnection.start();
        } catch (JMSException e) {
            LOG.error("Unable to start connection", e);
            closeSafely(jmsConnection);
            jmsConnection = null;
        }
    }

    @Hidden
    @PreDestroy
    public void shutdown() {
        closeSafely(jmsConnection);
    }

    
    @Hidden
    public void publish(EventMetadata metadata, EventPayload payload) {
        String message = eventSerializer.serialize(metadata, payload).toString();
        
        publish(message, metadata);
    }

    @Prototype
    public void publishMessage(@Named("Message") String messageStr) {
        if(jmsConnection == null) {
            throw new ApplicationException("Unable to publish (init failed, no JMS connection)");
        }
        
        publish(messageStr, null);
    }

    private void publish(String messageStr, EventMetadata metadata) {
        Session session = null;
        try {
            session = jmsConnection.createSession(transacted, Session.SESSION_TRANSACTED);
            Destination destination = session.createQueue(queue);
    
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            
            TextMessage message = session.createTextMessage(messageStr);
            if(metadata != null) {
                message.setJMSMessageID(metadata.getId());
            }
    
            LOG.info("Sent message: "+ message.hashCode());
            producer.send(message);
            
            session.commit();
    
        } catch (JMSException e) {
            try {
                session.rollback();
            } catch (JMSException e2) {
                // ignore
            }
            throw new ApplicationException("Failed to publish message", e);
        } finally {
            if(session != null) {
                closeSafely(session);
            }
        }
    }

    

    ///////////////////////////////////////////////////
    // Helper
    ///////////////////////////////////////////////////
    
    private static void closeSafely(Connection connection) {
        if(connection != null) {
            try {
                connection.close();
            } catch (JMSException e) {
                //ignore
            }
        }
    }
    
    private static void closeSafely(Session session) {
        try {
            session.close();
        } catch (JMSException e) {
            // ignore
        }
    }

    
    ///////////////////////////////////////////////////
    // Dependencies
    ///////////////////////////////////////////////////

    
    @Hidden
    public void setEventSerializer(EventSerializer eventSerializer) {
        this.eventSerializer = eventSerializer;
    }




}
