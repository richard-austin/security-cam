package com.proxy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.activemq.ActiveMQSslConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.proxy.CloudAMQProxy.MessageMetadata.*;


public class CloudAMQProxy {
    public enum MessageMetadata {
        HEARTBEAT("heartbeat"),
        REQUEST_RESPONSE("requestResponse"),
        TOKEN("token"),
        CONNECTION_CLOSED("connectionClosed"),
        CLOUD_PROXY_CORRELATION_ID("cloudProxy");
        final String value;

        MessageMetadata(String value) {
            this.value = value;
        }
    }

    final Map<Integer, SocketChannel> tokenSocketMap = new ConcurrentHashMap<>();

    private boolean running = false;
    final private static Queue<ByteBuffer> bufferQueue = new ConcurrentLinkedQueue<>();
    public static final int BUFFER_SIZE = 16384;
    private final String webServerForCloudProxyHost;
    private final int webServerForCloudProxyPort;

    private static final Logger logger = (Logger) LoggerFactory.getLogger("CLOUDPROXY");
    private ExecutorService cloudProxyExecutor;
    private ExecutorService webserverReadExecutor;
    private ExecutorService webserverWriteExecutor;
    private static final String productIdRegex = "^(?:[A-Z0-9]{4}-){3}[A-Z0-9]{4}$";
    CloudProxyProperties cloudProxyProperties = CloudProxyProperties.getInstance();
    final Object LOCK = new Object();
    private Session session = null;
    private String productId = "";

    public CloudAMQProxy(String webServerForCloudProxyHost, int webServerForCloudProxyPort) {
        this.webServerForCloudProxyHost = webServerForCloudProxyHost;
        this.webServerForCloudProxyPort = webServerForCloudProxyPort;
    }

    public void start() {
        if (!running) {
            setLogLevel(cloudProxyProperties.getLOG_LEVEL());

            cloudProxyExecutor = Executors.newSingleThreadExecutor();
            webserverReadExecutor = Executors.newCachedThreadPool();
            webserverWriteExecutor = Executors.newSingleThreadExecutor();
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

    public void stop() {
        if (running) {
            running = false;
            stopCloudInputProcess();
            synchronized (LOCK) {
                LOCK.notify();
            }
        }
    }

    public boolean isRunning() {
        return running;
    }


    private void createConnectionToCloud() {
        try {
            Session session = getSession();

            // Create the destination
            Destination destination = session.createQueue(cloudProxyProperties.getACTIVE_MQ_INIT_QUEUE());
            createCloudProxySessionTimer();     // Start the connection timer, if heartbeats are not received for the
            // timout period, this will trigger a retry.
            // If we fail to connect,this time, the timeout will trigger a retry
            // after the timeout period.

            if (productKeyAccepted(session, destination)) {
                this.session = session;
                logger.info("Connected successfully to the Cloud");
                startCloudInputProcess();
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
    }

    void removeSocket(int token) {
        try (var channel = tokenSocketMap.remove(token)) {
            if (channel != null) {
                channel.close();
                channel.socket().close();
            }
            logger.debug("Removing and closing socket for token " + token);
        } catch (Exception ex) {
            showExceptionDetails(ex, "removeSocket");
        }
    }

    private void writeRequestToWebserver(BytesMessage message) {
        try {
            logger.debug("Received message ");
            int token = message.getIntProperty(TOKEN.value);
            if (tokenSocketMap.containsKey(token)) {
                SocketChannel webserverChannel = tokenSocketMap.get(token);
                writeRequestToWebserver(message, webserverChannel);
            } else { // Make a new connection to the webserver
                final SocketChannel webserverChannel = SocketChannel.open();
                webserverChannel.connect(new InetSocketAddress(webServerForCloudProxyHost, webServerForCloudProxyPort));
                webserverChannel.configureBlocking(true);
                tokenSocketMap.put(token, webserverChannel);
                logger.debug("writeRequestToWebserver(1) length: " + message.getBodyLength());
                writeRequestToWebserver(message, webserverChannel);
                readResponseFromWebserver(webserverChannel, token);
            }
        } catch (Exception ex) {
            showExceptionDetails(ex, "writeRequestToWebserver");
            restart();
        }
    }

    private void writeRequestToWebserver(final BytesMessage msg, final SocketChannel webserverChannel) {
        this.webserverWriteExecutor.submit(() -> {
            try {
                int length = (int) msg.getBodyLength();
                ByteBuffer buf = length > BUFFER_SIZE ? ByteBuffer.allocate(length) : getBuffer();
                msg.readBytes(buf.array());
                buf.limit(length);
                int result;
                logger.debug("writeRequestToWebserver(2) length: " + msg.getBodyLength());
                do {
                    result = webserverChannel.write(buf);
                }
                while (result != -1 && buf.position() < buf.limit());

                if (length <= BUFFER_SIZE)
                    recycle(buf);

                // Don't recycle the dynamically created buffer as it will cause a build up in the buffer qeue// These are left to be cleared up by Java housekeeping.
            } catch (ClosedChannelException ignored) {
                try {
                    // Close the channel or the socket will be left in the CLOSE-WAIT state
                    webserverChannel.close();
                } catch (Exception ignore) {
                }
            } catch (Exception ex) {
                showExceptionDetails(ex, "writeRequestToWebserver");
            }
        });
    }

    private void readResponseFromWebserver(SocketChannel webserverChannel, int token) {
        webserverReadExecutor.submit(() -> {
            try {
                ByteBuffer buf = getBuffer();
                while (running && webserverChannel.isOpen() && webserverChannel.read(buf) != -1) {
                    logger.debug("readResponseFromWebserver length: " + buf.position());
                    final BytesMessage msg = session.createBytesMessage();
                    msg.writeBytes(buf.array(), 0, buf.position());
                    msg.setIntProperty(TOKEN.value, token);
                    msg.setJMSCorrelationID(CLOUD_PROXY_CORRELATION_ID.value);
                    cip.sendResponseToCloud(msg);
                    recycle(buf);
                    buf = getBuffer();
                }
                recycle(buf);
                webserverChannel.close();
            } catch (AsynchronousCloseException ignored) {
                // Don't report AsynchronousCloseException as these come up when the channel has been closed
                //  by a signal via getConnectionClosedFlag  from Cloud
            } catch (Exception e) {
                showExceptionDetails(e, "readResponseFromWebserver");
            }
        });
    }

    private class CloudInputProcess implements MessageListener {
        final private Session session;
        Destination cloud = null;
        MessageProducer producer = null;

        CloudInputProcess(Session session) {
            this.session = session;
        }

        MessageConsumer cons = null;

        void start() {
            try {

                Destination dest = session.createTopic(productId);
                cons = session.createConsumer(dest);
                cons.setMessageListener(this);

            } catch (JMSException ex) {
                logger.error("JMS Exception in CloudInputProcess.start(): " + ex.getMessage());
            }
        }

        void stop() {
            try {
                cons.close();
                cloud = null;
                producer = null;
            } catch (Exception ex) {
                logger.error(ex.getClass().getName() + " in CloudInputProcess,stop: " + ex.getMessage());
            }
        }

        @Override
        public void onMessage(Message message) {
            try {
                if (cloud == null) {
                    cloud = message.getJMSReplyTo();
                    producer = session.createProducer(cloud);
//                    producer.setDisableMessageID(true);
//                    producer.setDisableMessageTimestamp(true);
                    //      producer.setTimeToLive(1000);
                }

                if (message.getBooleanProperty(HEARTBEAT.value)) {
                    logger.debug("Received heartbeat");
                    sendResponseToCloud(message);  // Bounce heartbeats back to the Cloud
                    resetCloudProxySessionTimeout();
                } else if (message.getBooleanProperty(CONNECTION_CLOSED.value))
                    removeSocket(message.getIntProperty(TOKEN.value));
                else if (message instanceof BytesMessage)
                    writeRequestToWebserver((BytesMessage) message);
                else
                    logger.error("Unhandled message type in CloudInputProcess.onMessage: " + message.getClass().getName());
            } catch (Exception ex) {
                cloud = null;
                producer = null;
                logger.error(ex.getClass().getName() + " in CloudInputProcess.onMessage: " + ex.getMessage());
            }
        }

        void sendResponseToCloud(Message msg) {
            try {
                if (producer != null)
                    producer.send(msg);
            } catch (Exception ex) {
                cloud = null;
                producer = null;
                logger.error(ex.getClass().getName() + " in CloudInputProcess.sendResponseToCloud: " + ex.getMessage());
            }
        }
    }

    CloudInputProcess cip = null;

    private void startCloudInputProcess() {
        cip = new CloudInputProcess(session);
        cip.start();
    }

    private void stopCloudInputProcess() {
        try {
            cip.stop();
            session.close();
        } catch (Exception ex) {
            logger.error(ex.getClass().getName() + " in stopCloudInputProcess: " + ex.getMessage());
        }
    }

    private Session getSession() throws Exception {
        ActiveMQSslConnectionFactory connectionFactory = new ActiveMQSslConnectionFactory(cloudProxyProperties.getCLOUD_PROXY_ACTIVE_MQ_URL());
        connectionFactory.setUseAsyncSend(true);
        connectionFactory.setOptimizeAcknowledge(true);
        // connectionFactory.setAlwaysSessionAsync(false);

        connectionFactory.setKeyStore(cloudProxyProperties.getMQ_CLOUD_PROXY_KEYSTORE_PATH());
        connectionFactory.setKeyStorePassword(cloudProxyProperties.getMQ_CLOUD_PROXY_KEYSTORE_PASSWORD());
        connectionFactory.setTrustStore(cloudProxyProperties.getMQ_TRUSTSTORE_PATH());
        connectionFactory.setTrustStorePassword(cloudProxyProperties.getMQ_TRUSTSTORE_PASSWORD());
        // Create a Connection
        Connection connection = connectionFactory.createConnection(cloudProxyProperties.getMQ_USER(), cloudProxyProperties.getMQ_PASSWORD());
        connection.start();

        // Create a Session
        return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
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
            producer.send(message, DeliveryMode.NON_PERSISTENT, 4, 1000);
            // Get the response from the Cloud
            MessageConsumer consumer = session.createConsumer(replyQ);
            Message response = consumer.receive(1000);
            if (response instanceof ActiveMQTextMessage tm && response.getBooleanProperty("INIT_RESPONSE") && Objects.equals(response.getJMSCorrelationID(), "initResponseCorrelationId")) {
                productId = tm.getText();
                if (productId.matches(productIdRegex)) {
                    retVal = true;
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getClass().getName() + " in productKeyAccepted: " + ex.getMessage());
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
        // Restart CloudAMQProxy after 20 seconds without a heartbeat
        long cloudProxySessionTimeout = 20 * 1000;
        cloudProxySessionTimer.schedule(cstt, cloudProxySessionTimeout);
    }

    public void resetCloudProxySessionTimeout() {

        if (cloudProxySessionTimer != null)
            cloudProxySessionTimer.cancel();

        createCloudProxySessionTimer();
    }

    /**
     * cleanUpForRestart: Some sort of problem occurred with the Cloud connection, ensure we restart cleanly
     */
    void restart() {
        if (running) {
            try {
                setLogLevel(cloudProxyProperties.getLOG_LEVEL());
                logger.info("Restarting CloudAMQProxy");
                webserverWriteExecutor.shutdownNow();
                webserverWriteExecutor = Executors.newSingleThreadExecutor();

                // Ensure all sockets in the token/socket map are closed
                tokenSocketMap.forEach((token, socket) -> {
                    try {
                        socket.close();
                    } catch (IOException ignore) {
                    }
                });
                // Clear the token/socket map
                tokenSocketMap.clear();
                if (session != null) {
                    session.close();
                    session = null;
                }
                // Restart the start process
                new Thread(this::createConnectionToCloud).start();
            } catch (Exception ex) {
                logger.error(ex.getClass().getName() + " in restart: " + ex.getMessage());
            }
        }
        /**/
    }

    /**
     * getBuffer: Get a new ByteBuffer of BUFFER_SIZE bytes length.
     *
     * @return: The buffer
     */
    public static ByteBuffer getBuffer() {
        ByteBuffer buf = Objects.requireNonNullElseGet(bufferQueue.poll(), () -> ByteBuffer.allocate(BUFFER_SIZE));
        buf.clear();
        return buf;
    }

    public static synchronized void recycle(ByteBuffer buf) {
        buf.clear();
        bufferQueue.add(buf);
    }

    void showExceptionDetails(Throwable t, String functionName) {
        logger.error(t.getClass().getName() + " exception in " + functionName + ": " + t.getMessage());
//        for (StackTraceElement stackTraceElement : t.getStackTrace()) {
//            System.err.println(stackTraceElement.toString());
//        }
    }
}
