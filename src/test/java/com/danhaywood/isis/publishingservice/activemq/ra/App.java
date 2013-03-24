package com.danhaywood.isis.publishingservice.activemq.ra;

import java.util.List;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.google.common.collect.Lists;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Hello world!
 */
public class App {

    private static ActiveMQConnectionFactory connectionFactory;
    private static Connection connection;
    private static Connection connection2;
    private static List<Thread> threads = Lists.newArrayList();

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        Logger root = Logger.getRootLogger();
        root.setLevel(Level.WARN);

        String brokerURL = "nio://0.0.0.0:61616";
        connectionFactory = new ActiveMQConnectionFactory(brokerURL);
        
        connection = connectionFactory.createConnection();
        connection.start();
        
        connection2 = connectionFactory.createConnection();
        connection2.start();

        connection2.setExceptionListener(new MyExceptionListener());

//        thread(new HelloWorldProducer(), false);
//        thread(new HelloWorldProducer(), false);
//        thread(new HelloWorldConsumer(), false);
//        Thread.sleep(1000);
//        thread(new HelloWorldConsumer(), false);
//        thread(new HelloWorldProducer(), false);
//        thread(new HelloWorldConsumer(), false);
//        thread(new HelloWorldProducer(), false);
//        Thread.sleep(1000);
//        thread(new HelloWorldConsumer(), false);
//        thread(new HelloWorldProducer(), false);
//        thread(new HelloWorldConsumer(), false);
//        thread(new HelloWorldConsumer(), false);
//        thread(new HelloWorldProducer(), false);
//        thread(new HelloWorldProducer(), false);
//        Thread.sleep(1000);
//        thread(new HelloWorldProducer(), false);
//        thread(new HelloWorldConsumer(), false);
//        thread(new HelloWorldConsumer(), false);
//        thread(new HelloWorldProducer(), false);
//        thread(new HelloWorldConsumer(), false);
//        thread(new HelloWorldProducer(), false);
//        thread(new HelloWorldConsumer(), false);
//        thread(new HelloWorldProducer(), false);
//        thread(new HelloWorldConsumer(), false);
//        thread(new HelloWorldConsumer(), false);
//        thread(new HelloWorldProducer(), false);

//        thread(new HelloWorldProducer(), false);
        thread(new HelloWorldConsumer(), false);

        for (Thread thread : threads) {
            thread.join();
        }
        connection.close();
        connection2.close();
        
    }

    public static Thread thread(Runnable runnable, boolean daemon) {
        Thread brokerThread = new Thread(runnable);
        brokerThread.setDaemon(daemon);
        brokerThread.start();
        threads.add(brokerThread);
        return brokerThread;
    }

    public static class HelloWorldProducer implements Runnable {
        
        public void run() {
            try {
                // Create a Session
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                // Create the destination (Topic or Queue)
                Destination destination = session.createQueue("estatio-published-events");

                // Create a MessageProducer from the Session to the Topic or Queue
                MessageProducer producer = session.createProducer(destination);
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

                // Create a messages
                String text = "Hello world! From: " + Thread.currentThread().getName() + " : " + this.hashCode();
                TextMessage message = session.createTextMessage(text);

                // Tell the producer to send the message
                System.out.println("Sent message: "+ message.hashCode() + " : " + Thread.currentThread().getName());
                producer.send(message);

                // Clean up
                session.close();
            }
            catch (Exception e) {
                System.out.println("Caught: " + e);
                e.printStackTrace();
            }
        }
    }

    public static class HelloWorldConsumer implements Runnable, ExceptionListener {
        public void run() {
            try {


                // Create a Session
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                // Create the destination (Topic or Queue)
                Destination destination = session.createQueue("estatio-published-events");

                // Create a MessageConsumer from the Session to the Topic or Queue
                MessageConsumer consumer = session.createConsumer(destination);

                // Wait for a message
                Message message = consumer.receive(1000);

                if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;
                    String text = textMessage.getText();
                    System.out.println("Received: " + text);
                } else {
                    System.out.println("Received: " + message);
                }

                consumer.close();
                session.close();
            } catch (Exception e) {
                System.out.println("Caught: " + e);
                e.printStackTrace();
            }
        }

        public synchronized void onException(JMSException ex) {
            System.out.println("JMS Exception occured.  Shutting down client.");
        }
    }

    public static class MyExceptionListener implements ExceptionListener {
        public synchronized void onException(JMSException ex) {
            System.out.println("JMS Exception occured.  Shutting down client.");
        }
    }

}