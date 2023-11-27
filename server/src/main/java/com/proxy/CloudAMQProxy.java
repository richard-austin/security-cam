package com.proxy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.activemq.ActiveMQSslConnectionFactory;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CloudAMQProxy implements MessageListener {
    private boolean running = false;
    private final long cloudProxySessionTimeout = 50 * 1000; // Restart CloudProxy after 50 seconds without a heartbeat

    private static final Logger logger = (Logger) LoggerFactory.getLogger("CLOUDPROXY");
    private ExecutorService cloudProxyExecutor;
    AdvisoryMonitor am;
    private static final String productIdRegex = "^(?:[A-Z0-9]{4}-){3}[A-Z0-9]{4}$";

    CloudProxyProperties cloudProxyProperties = CloudProxyProperties.getInstance();
    final Object LOCK = new Object();
    private Session session = null;

    public void start() {
        if (!running) {
            setLogLevel(cloudProxyProperties.getLOG_LEVEL());

            cloudProxyExecutor = Executors.newSingleThreadExecutor();
            cloudProxyExecutor.execute(() -> {
                running = true;
                try {
                    createConnectionToCloud();

                    synchronized (LOCK) {
                        try {
                            LOCK.wait();
                        } catch (InterruptedException e) {
                            showExceptionDetails(e, "start");
                        }
                    }
                } catch (Exception ex) {
                    showExceptionDetails(ex, "start");
                }
            });
        }
    }

    @Override
    public void onMessage(Message message) {

    }

    private void createConnectionToCloud() {
        try {
                Session session = getSession();

                // Create the destination
                Destination destination = session.createQueue(cloudProxyProperties.getACTIVE_MQ_INIT_QUEUE());
    //            am = new AdvisoryMonitor(session, destination);


                createCloudProxySessionTimer();   // Start the connection timer, if heartbeats are not received for the
                // timout period, this will trigger a retry.
                // If we fail to connect,this time, the timeout will trigger a retry
                // after the timeout period.

                if (productKeyAccepted(session, destination)) {
                    this.session = session;
                    logger.info("Connected successfully to the Cloud");
                    startCloudInputProcess(session);
                } else
                    logger.error("Product key was not accepted by the Cloud server");

        } catch (Exception e) {
            logger.warn("Exception in createConnectionToCloud: " + e.getMessage() + ": Couldn't connect to Cloud");
            if (this.session != null) {
                try {
                    this.session.close();
                    this.session = null;
                } catch (JMSException ignored) {
                }
            }
        }
        startCloudConnectionCheck();
    }

    private void startCloudConnectionCheck() {

    }
    private void startCloudInputProcess(Session  session) {

    }

    private Session getSession() throws Exception {
        ActiveMQSslConnectionFactory connectionFactory = new ActiveMQSslConnectionFactory("failover://ssl://localhost:61617?socket.verifyHostName=false");
        connectionFactory.setKeyStore(cloudProxyProperties.getMQ_CLOUD_PROXY_KEYSTORE_PATH());
        connectionFactory.setKeyStoreKeyPassword(cloudProxyProperties.getMQ_CLOUD_PROXY_KEYSTORE_PASSWORD());
        connectionFactory.setTrustStore(cloudProxyProperties.getMQ_TRUSTSTORE_PATH());
        connectionFactory.setTrustStorePassword(cloudProxyProperties.getMQ_TRUSTSTORE_PASSWORD());
        // Create a Connection
        Connection connection = connectionFactory.createConnection();
        connection.start();

        // Create a Session
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        return session;
    }

    /**
     * productKeyAccepted: Send the product key to the Cloud server and check it was accepted
     *
     * @param session:     ActiveMQ session for initialisation
     * @param destination: ActiveMQ destination for initialisation
     * @return: true if product key accepted, else false
     */
    private boolean productKeyAccepted(Session session, Destination destination) {
        boolean retVal = false;
        // Get the product key string

        try {
            final String prodKey = Files.readString(new File(cloudProxyProperties.getPRODUCT_KEY_PATH()).toPath());
            // Send the encrypted product key out
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            BytesMessage message = session.createBytesMessage();
            message.writeBytes(prodKey.getBytes(StandardCharsets.UTF_8));
            message.setBooleanProperty("INIT", true);
            Destination replyQ = session.createTemporaryQueue();
            message.setJMSReplyTo(replyQ);
            message.setJMSCorrelationID("initCorrelationId");
            producer.send(message);
            // Get thew response from the Cloud
            MessageConsumer consumer = session.createConsumer(replyQ);
            Message response = consumer.receive(1000);
            if (response instanceof BytesMessage && message.getBooleanProperty("INIT_RESPONSE") && Objects.equals(message.getJMSCorrelationID(), "initResponseCorrelationId")) {
                final byte[] result = new byte[10];
                final int bytesRead = message.readBytes(result);
                final String productId = new String(result, 0, bytesRead);
                if (productId.matches(productIdRegex))
                    retVal = true;
            }

        } catch (Exception ex) {
            logger.error(ex.getClass().getName()+" in productKeyAccepted: "+ex.getMessage());
        }
        return retVal;
    }

    Timer cloudProxySessionTimer;

    void setLogLevel(String level) {
        logger.setLevel(Objects.equals(level, "INFO") ? Level.INFO :
                Objects.equals(level, "DEBUG") ? Level.DEBUG :
                        Objects.equals(level, "TRACE") ? Level.TRACE :
                                Objects.equals(level, "WARN") ? Level.WARN :
                                        Objects.equals(level, "ERROR") ? Level.ERROR :
                                                Objects.equals(level, "OFF") ? Level.OFF :
                                                        Objects.equals(level, "ALL") ? Level.ALL : Level.OFF);
    }


    private void createCloudProxySessionTimer() {
        if (cloudProxySessionTimer != null)
            cloudProxySessionTimer.cancel();
        CloudSessionTimerTask cstt = new CloudSessionTimerTask(this);
        cloudProxySessionTimer = new Timer("cloudProxySessionTimer");
        cloudProxySessionTimer.schedule(cstt, cloudProxySessionTimeout);
    }

    public void resetCloudProxySessionTimeout() {

        if (cloudProxySessionTimer != null)
            cloudProxySessionTimer.cancel();

        createCloudProxySessionTimer();
    }
    void restart() {
        if (running) {
        }
    }
    void showExceptionDetails(Throwable t, String functionName) {
        logger.error(t.getClass().getName() + " exception in " + functionName + ": " + t.getMessage());
//        for (StackTraceElement stackTraceElement : t.getStackTrace()) {
//            System.err.println(stackTraceElement.toString());
//        }
    }
}
