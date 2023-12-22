package com.proxy;

import org.apache.activemq.advisory.AdvisorySupport;
import org.apache.activemq.command.*;

import javax.jms.*;
import javax.jms.Message;

public class AdvisoryMonitor implements MessageListener {
    Session session;
    Destination destination;
    AdvisoryMonitor(Session session, Destination destination) {
        this.session = session;
        this.destination = destination;
        setup();
    }
    Destination advisoryDestination;
    MessageConsumer consumer;
    private void setup() {
        try {
            advisoryDestination = AdvisorySupport.getConnectionAdvisoryTopic();
            consumer = session.createConsumer(advisoryDestination);
            consumer.setMessageListener(this);
        }
        catch(JMSException ex) {
            System.out.println("Exception: "+ex.getMessage());
        }
    }

    public void onMessage(Message msg){
        if(msg instanceof ActiveMQMessage) {
            try {
                ActiveMQMessage aMsg = (ActiveMQMessage)msg;
                DataStructure dataStructure = aMsg.getDataStructure();
                if(dataStructure instanceof ProducerInfo) {
                    Object obj = (ProducerInfo)dataStructure;
                }
                else if(dataStructure instanceof ConsumerInfo) {
                    ConsumerInfo cons = (ConsumerInfo) aMsg.getDataStructure();
                    System.out.println("ConsumerInfo Message received: consumerCount = " + aMsg.getIntProperty("consumerCount"));
                }
                else if(dataStructure instanceof RemoveInfo) {
                    RemoveInfo ri = (RemoveInfo) dataStructure;
                    System.out.println("RemoveInfo Message received: consumerCount = " + aMsg.getIntProperty("consumerCount"));
                }
                else if (dataStructure instanceof ConnectionInfo) {
                    ConnectionInfo ci = (ConnectionInfo) dataStructure;
                    if(aMsg.getConnection().isClosed())
                        System.out.println("Connection closed");
                    else
                        System.out.println("Connection open");
                    System.out.println("ConnectionInfo Message received: consumerCount = ");
                }
                else
                    System.out.println("Something else");
            }
            catch(Exception e) {
                //  log.error("Failed to process message: " + msg);
            }
        }
        else
            System.out.println("Something else");
    }
}
