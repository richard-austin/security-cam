package com.proxy;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static com.proxy.SslUtil.*;

public class CloudProxy implements SslContextProvider {
    final Map<Integer, SocketChannel> tokenSocketMap = new ConcurrentHashMap<>();
    private final int tokenLength = Integer.BYTES;
    private final int lengthLength = Integer.BYTES;
    private final int closedFlagLength = Byte.BYTES;
    private final int headerLength = tokenLength + lengthLength + closedFlagLength;
    public static final int BUFFER_SIZE = 16384;
    private static final Logger logger = (Logger) LoggerFactory.getLogger("CLOUDPROXY");
    final Queue<ByteBuffer> bufferQueue = new ConcurrentLinkedQueue<>();
    SSLSocket cloudChannel;
    private boolean running = false;
    private final String webServerForCloudProxyHost;
    private final int webServerForCloudProxyPort;
    private final String cloudHost;
    private final int cloudPort;
    CloudProxyProperties cloudProxyProperties = CloudProxyProperties.getInstance();

    private ExecutorService cloudProxyExecutor;
    private ExecutorService splitMessagesExecutor;
    private ExecutorService webserverReadExecutor;
    private ExecutorService webserverWriteExecutor;
    private ExecutorService sendResponseToCloudExecutor;
    private ScheduledExecutorService cloudConnectionCheckExecutor;
    private ExecutorService startCloudInputProcessExecutor;

    public CloudProxy(String webServerForCloudProxyHost, int webServerForCloudProxyPort, String cloudHost, int cloudPort) {
        this.webServerForCloudProxyHost = webServerForCloudProxyHost;
        this.webServerForCloudProxyPort = webServerForCloudProxyPort;
        this.cloudHost = cloudHost;
        this.cloudPort = cloudPort;
    }

    final Object LOCK = new Object();

    public void start() {
        if (!running) {
            setLogLevel(cloudProxyProperties.getLOG_LEVEL());
            cloudProxyExecutor = Executors.newSingleThreadExecutor();
            splitMessagesExecutor = Executors.newSingleThreadExecutor();
            webserverReadExecutor = Executors.newCachedThreadPool();
            webserverWriteExecutor = Executors.newSingleThreadExecutor();
            sendResponseToCloudExecutor = Executors.newSingleThreadExecutor();
            cloudConnectionCheckExecutor = Executors.newSingleThreadScheduledExecutor();
            startCloudInputProcessExecutor = Executors.newSingleThreadExecutor();

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

    public void stop() throws IOException {
        if (running) {
            running = false;
            try {
                if (cloudChannel != null && !cloudChannel.isClosed())
                    cloudChannel.close();
            } catch (Exception ignored) {
            }

            splitMessagesExecutor.shutdownNow();
            webserverReadExecutor.shutdownNow();
            webserverWriteExecutor.shutdownNow();
            sendResponseToCloudExecutor.shutdownNow();
            cloudConnectionCheckExecutor.shutdownNow();
            startCloudInputProcessExecutor.shutdownNow();
            synchronized (LOCK) {
                LOCK.notify();
            }
            cloudProxyExecutor.shutdownNow();
        }
    }

    public boolean isRunning() {
        return running;
    }

    private void createConnectionToCloud() {
        try {
            if (this.cloudChannel == null || !this.cloudChannel.isConnected() || this.cloudChannel.isClosed()) {
                createCloudProxySessionTimer();   // Start the connection timer, if heartbeats are not received for the
                // timout period, this will trigger a retry.
                // If we fail to connect,this time, the timeout will trigger a retry
                // after the timeout period.

                SSLSocket cloudChannel = createSSLSocket(cloudHost, cloudPort, this);
                if (productKeyAccepted(cloudChannel)) {
                    this.cloudChannel = cloudChannel;
                    logger.info("Connected successfully to the Cloud");
                    startCloudInputProcess(cloudChannel);
                } else
                    logger.error("Product key was not accepted by the Cloud server");
            }

        } catch (Exception e) {
            logger.warn("Exception in createConnectionToCloud: " + e.getMessage() + ": Couldn't connect to Cloud");
            if (this.cloudChannel != null) {
                try {
                    this.cloudChannel.close();
                } catch (IOException ignored) {
                }
            }
        }
        startCloudConnectionCheck();
    }

    /**
     * productKeyAccepted: Send the product key to the Cloud server and check it was accepted
     *
     * @param cloudChannel: The SSLSocket connected to the Cloud
     * @return: true if product key was accepted, otherwise false
     */
    private boolean productKeyAccepted(SSLSocket cloudChannel) throws IOException {
        boolean retVal = false;
        // Get the product key string
        final String prodKey = Files.readString(new File(cloudProxyProperties.getPRODUCT_KEY_PATH()).toPath());

        OutputStream os = cloudChannel.getOutputStream();
        os.write(prodKey.getBytes(StandardCharsets.UTF_8));
        os.flush();
        InputStream is = cloudChannel.getInputStream();
        final byte[] result = new byte[10];
        int bytesRead = is.read(result);

        if ((new String(result, 0, bytesRead)).equals("OK"))
            retVal = true;

        return retVal;
    }

    Timer cloudProxySessionTimer;

    private void createCloudProxySessionTimer() {
        if (cloudProxySessionTimer != null)
            cloudProxySessionTimer.cancel();
//        CloudSessionTimerTask cstt = new CloudSessionTimerTask(this);
//        cloudProxySessionTimer = new Timer("cloudProxySessionTimer");
//        cloudProxySessionTimer.schedule(cstt, cloudProxySessionTimeout);
    }

    public void resetCloudProxySessionTimeout() {

        if (cloudProxySessionTimer != null)
            cloudProxySessionTimer.cancel();

        createCloudProxySessionTimer();
    }

    private boolean isHeartbeat(ByteBuffer buf) {
        final int position = buf.position();
        boolean retVal = false;
        // Dump the connection test heartbeats
        final String ignored = "Ignore";
        if (getToken(buf) == -1 && getDataLength(buf) == ignored.length()) {
            String strVal = new String(Arrays.copyOfRange(buf.array(), headerLength, buf.limit()), StandardCharsets.UTF_8);
            if (ignored.equals(strVal)) {
                retVal = true;
            }
        }
        buf.position(position);
        return retVal;
    }

    private void startCloudInputProcess(SSLSocket cloudChannel) {
        final AtomicBoolean busy = new AtomicBoolean(false);
        startCloudInputProcessExecutor.execute(() -> {
            try {
                if (!busy.get()) {
                    busy.set(true);
                    InputStream is = cloudChannel.getInputStream();
                    ByteBuffer buf = getBuffer();
                    while (read(is, buf) != -1) {
                        logger.trace("startCloudInputProcess read pos: " + buf.position() + " length: " + buf.limit());
                        if (!isHeartbeat(buf))
                            writeRequestToWebserver(buf);
                        else {
                            resetCloudProxySessionTimeout();
                            logger.debug("Heartbeat received");
                        }
                        buf = getBuffer();
                    }
                    busy.set(false);
                    busy.set(false);
                    logger.error("Cloud reader socket has closed in startCloudInputProcess");
                }
            } catch (Exception ex) {
                showExceptionDetails(ex, "startCloudInputProcess");
                //   restart();  // The session timeout will restart it, this will only result in 2 restarts.
            }
        });
    }

    private void startCloudConnectionCheck() {
        try {
            final ByteBuffer buf = getBuffer(-1);
            buf.put("Ignore".getBytes(StandardCharsets.UTF_8));
            setDataLength(buf, buf.position() - headerLength);

            cloudConnectionCheckExecutor.scheduleAtFixedRate(() -> {
                try {
                    OutputStream os = cloudChannel.getOutputStream();
                    if (cloudChannel != null && cloudChannel.isConnected() && !cloudChannel.isClosed()) {
                        setBufferForSend(buf);
                        write(os, buf);  // This will be ignored by the Cloud, just throws an exception if the link is down
                    } else throw new Exception("Not connected");
                } catch (NullPointerException ignored) {
                    logger.warn("cloudChannel is null, Cloud connection is down");
                    restart();
                } catch (Exception ex) {
                    logger.error("Exception in cloudConnectionCheck: " + ex.getMessage());
                    if (cloudChannel != null && !cloudChannel.isClosed()) {
                        try {
                            cloudChannel.close();
                        } catch (IOException ignored) {
                        }
                    }
                    restart();
                }
            }, 10, 10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            showExceptionDetails(ex, "startCloudConnectionCheck");
            restart();
        }
    }

    void removeSocket(int token) {
        try {
            SocketChannel sock = tokenSocketMap.get(token);
            sock.close();
            tokenSocketMap.remove(token);
        } catch (Exception ex) {
            showExceptionDetails(ex, "removeSocket");
        }
    }

    /**
     * cleanUpForRestart: Some sort of problem occurred with the Cloud connection, ensure we restart cleanly
     */
    void restart() {
        if (running) {
            try {
                setLogLevel(cloudProxyProperties.getLOG_LEVEL());
                logger.info("Restarting CloudProxy");
                sendResponseToCloudExecutor.shutdownNow();
                startCloudInputProcessExecutor.shutdownNow();
                cloudConnectionCheckExecutor.shutdownNow();
                webserverWriteExecutor.shutdownNow();

                sendResponseToCloudExecutor = Executors.newSingleThreadExecutor();
                startCloudInputProcessExecutor = Executors.newSingleThreadExecutor();
                cloudConnectionCheckExecutor = Executors.newSingleThreadScheduledExecutor();
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
                remainsOfPreviousBuffer = null;
                // Ensure the connection is actually closed
                if (cloudChannel != null && cloudChannel.isConnected() && !cloudChannel.isClosed()) {
                    try {
                        cloudChannel.close();
                    } catch (IOException ignored) {
                    }
                }
                cloudChannel = null;

                // Restart the start process
                new Thread(this::createConnectionToCloud).start();
            } catch (Exception ex) {
                showExceptionDetails(ex, "restart");
            }
        }
        /**/
    }

    private void writeRequestToWebserver(ByteBuffer buf) {
        try {
            logger.info("Received message ");
            int token = getToken(buf);
            if (tokenSocketMap.containsKey(token)) {
                if (getConnectionClosedFlag(buf) != 0) {
                    tokenSocketMap.get(token).close();
                    removeSocket(token);
                } else {
                    SocketChannel webserverChannel = tokenSocketMap.get(token);
                    writeRequestToWebserver(buf, webserverChannel);
                }
            } else  // Make a new connection to the webserver
            {
                final SocketChannel webserverChannel = SocketChannel.open();
                webserverChannel.connect(new InetSocketAddress(webServerForCloudProxyHost, webServerForCloudProxyPort));
                webserverChannel.configureBlocking(true);
                tokenSocketMap.put(token, webserverChannel);
                logger.debug("writeRequestToWebserver(1) length: " + getDataLengthFullRestore(buf));
                writeRequestToWebserver(buf, webserverChannel);
                readResponseFromWebserver(webserverChannel, token);
            }
        } catch (Exception ex) {
            showExceptionDetails(ex, "writeRequestToWebserver");
            restart();
        }
    }

    private void writeRequestToWebserver(final ByteBuffer buf, final SocketChannel webserverChannel) {
        this.webserverWriteExecutor.submit(() -> {
            //  logMessageMetadata(buf, "To webserv");
            try {
                int length = getDataLength(buf);
                buf.position(headerLength);
                buf.limit(headerLength + length);
                int result;
                logger.debug("writeRequestToWebserver(2) length: " + getDataLengthFullRestore(buf));
                do {
                    result = webserverChannel.write(buf);
                }
                while (result != -1 && buf.position() < buf.limit());

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
                ByteBuffer buf = getBuffer(token);
                while (running && webserverChannel.isOpen() && webserverChannel.read(buf) != -1) {
                    setDataLength(buf, buf.position() - headerLength);
                    sendResponseToCloud(buf);
                    buf = getBuffer(token);
                }
                logger.debug("readResponseFromWebserver length: " + getDataLengthFullRestore(buf));
                setConnectionClosedFlag(buf);
                sendResponseToCloud(buf);
                webserverChannel.close();
            } catch (AsynchronousCloseException ignored) {
                // Don't report AsynchronousCloseException as these come up when the channel has been closed
                //  by a signal via getConnectionClosedFlag  from Cloud
            } catch (Exception e) {
                showExceptionDetails(e, "readResponseFromWebserver");
            }
        });
    }

    private void sendResponseToCloud(ByteBuffer buf) {
        sendResponseToCloudExecutor.submit(() -> {
            boolean retVal = true;

            try {
                OutputStream os = cloudChannel.getOutputStream();
                setBufferForSend(buf);

                logger.debug("sendResponseToCloud length: " + getDataLengthFullRestore(buf));
                do {
                    write(os, buf);
                }
                while (buf.position() < buf.limit());
                recycle(buf);
            } catch (Exception ex) {
                showExceptionDetails(ex, "sendResponseToCloud");
                retVal = false;
            }
            return retVal;
        });
    }

    private int count = 0;
    private int lengthTotal = 0;
    private long checksumTotal = 0;

    private void logMessageMetadata(ByteBuffer buf, String title) {
        int position = buf.position();
        lengthTotal += getDataLength(buf);
        long checksum = getCRC32Checksum(buf);
        checksumTotal += checksum;
        boolean disconnect = getConnectionClosedFlag(buf) != 0;
        System.out.println(title + (disconnect ? "*" : ".") + ".   #: " + ++count + ", Token: " + getToken(buf) + ", Length: " + getDataLength(buf) + ", lengthTotal: " + lengthTotal + ", Checksum: " + checksum + ", ChecksumTotal: " + checksumTotal);
        buf.position(position);
    }

    /**
     * getBuffer: Get a new ByteBuffer of BUFFER_SIZE bytes length.
     *
     * @return: The buffer
     */
    private ByteBuffer getBuffer() {
        ByteBuffer buf = Objects.requireNonNullElseGet(bufferQueue.poll(), () -> ByteBuffer.allocate(BUFFER_SIZE));
        buf.clear();
        return buf;
    }

    /**
     * getBuffer: Get a buffer and place the token at the start. Reserve a further lengthLength bytes to contain the length.
     *
     * @param token: The token
     * @return: The byte buffer with the token in place and length reservation set up.
     */
    private ByteBuffer getBuffer(int token) {
        ByteBuffer buf = getBuffer();
        buf.putInt(token);
        buf.putInt(0);  // Reserve space for the data length
        buf.put((byte) 0); // Reserve space for the closed connection flag
        return buf;
    }

    private void recycle(ByteBuffer buf) {
        if(buf.capacity() == BUFFER_SIZE) {  // Don't recycle buffers created outside getBuffer()
            buf.clear();
            bufferQueue.add(buf);
        }
    }

    /**
     * setDataLength: Set the lengthLength bytes following the token to the length of the data in the buffer
     * (minus token and length bytes).
     *
     * @param buf:    The buffer to set the length in.
     * @param length: The length to set.
     */
    private void setDataLength(ByteBuffer buf, int length) {
        int position = buf.position();
        buf.position(tokenLength);
        buf.putInt(length);
        buf.position(position);
    }

    private void setConnectionClosedFlag(ByteBuffer buf) {
        buf.position(tokenLength + lengthLength);
        buf.put((byte) 1);
        setDataLength(buf, 0);
        buf.limit(headerLength);
    }

    private byte getConnectionClosedFlag(ByteBuffer buf) {
        int position = buf.position();
        buf.position(tokenLength + lengthLength);
        byte flag = buf.get();
        buf.position(position);
        return flag;
    }

    /**
     * getDataLength: Get the length of the data from the buffer. The actual data follows the token and length bytes.
     *
     * @param buf: The buffer
     * @return: The length of the data in the buffer
     */
    private int getDataLength(ByteBuffer buf) {
        int length = buf.getInt(tokenLength);
        buf.position(tokenLength + lengthLength);
        return length;
    }

    private int getDataLengthFullRestore(ByteBuffer buf) {
        int limit = buf.limit();
        int position = buf.position();
        int length = getDataLength(buf);
        buf.limit(limit);
        buf.position(position);

        return length;
    }

    /**
     * getToken: Get the token in the ByteBuffer
     *
     * @param buf: The buffer containing the token.
     * @return: The token
     */
    private int getToken(ByteBuffer buf) {
        if(buf.limit() >= Integer.BYTES) {
            int position = buf.position();
            buf.position(0);
            int token = buf.getInt();
            buf.position(position);
            return token;
        }
        else {
            logger.error("getToken was called with buffer length less than "+Integer.BYTES+" ("+buf.limit()+")");
            return 0;
        }
    }

    public long getCRC32Checksum(ByteBuffer buf) {
        int length = getDataLength(buf);
        Checksum crc32 = new CRC32();
        crc32.update(buf.array(), 0, length + headerLength);
        return crc32.getValue();
    }

    void setBufferForSend(ByteBuffer buf) {
        buf.flip();
    }

    private void write(OutputStream os, ByteBuffer buf) throws IOException {
        os.write(buf.array(), buf.position(), buf.limit() - buf.position());
        os.flush();
        buf.position(buf.limit());
    }

    private int read(InputStream is, ByteBuffer buf) throws IOException {
        final int retVal = is.read(buf.array(), buf.position(), buf.capacity() - buf.position());
        if (retVal != -1) {
            buf.limit(buf.position() + retVal);
            buf.position(buf.limit());
        }
        return retVal;
    }

    ByteBuffer remainsOfPreviousBuffer = null;

     void setLogLevel(String level) {
        logger.setLevel(Objects.equals(level, "INFO") ? Level.INFO :
                Objects.equals(level, "DEBUG") ? Level.DEBUG :
                        Objects.equals(level, "TRACE") ? Level.TRACE :
                                Objects.equals(level, "WARN") ? Level.WARN :
                                        Objects.equals(level, "ERROR") ? Level.ERROR :
                                                Objects.equals(level, "OFF") ? Level.OFF :
                                                        Objects.equals(level, "ALL") ? Level.ALL : Level.OFF);
    }

     void showExceptionDetails(Throwable t, String functionName) {
        logger.error(t.getClass().getName() + " exception in " + functionName + ": " + t.getMessage());
//        for (StackTraceElement stackTraceElement : t.getStackTrace()) {
//            System.err.println(stackTraceElement.toString());
//        }
    }

    @Override
    public KeyManager[] getKeyManagers() throws GeneralSecurityException, IOException {
 //       return createKeyManagers(cloudProxyProperties.getCLOUD_PROXY_KEYSTORE_PATH(), cloudProxyProperties.getCLOUD_PROXY_KEYSTORE_PASSWORD().toCharArray());
        return null;
    }

    @Override
    public String getProtocol() {
        return "TLSv1.2";
    }

    @Override
    public TrustManager[] getTrustManagers() throws GeneralSecurityException, IOException {
        return createTrustManagers(cloudProxyProperties.getMQ_TRUSTSTORE_PATH(), cloudProxyProperties.getMQ_TRUSTSTORE_PASSWORD().toCharArray());
    }

    private String log(ByteBuffer buf) {
        int position = buf.position();
        buf.position(tokenLength + lengthLength);

        int length = getDataLength(buf);
        byte[] dataBytes = new byte[length];
        for (int i = 0; i < length; ++i)
            dataBytes[i] = buf.get();
        buf.position(position);
        return new String(dataBytes);
    }
}
