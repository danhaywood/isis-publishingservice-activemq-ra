package com.danhaywood.isis.publishingservice.activemq.ra;

import java.util.Map;

import com.google.common.collect.Maps;

public class App2 {

    public static void main(String[] args) {
        PublishingServiceUsingActiveMqRa service = new PublishingServiceUsingActiveMqRa();
        Map<String, String> properties = Maps.newHashMap();
        properties.put("com.danhaywood.isis.publishingservice.activemq.ra.java.naming.provider.url", "nio://127.0.0.1:61616");
        service.init(properties);
        
        service.publishMessage("Foo");
        
        service.shutdown();
    }
    
}
