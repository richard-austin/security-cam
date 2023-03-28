package com.proxy;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class CamWebadminHostProxy {
    ILogService logService;
    ICamServiceInterface camService;
    final Map<String, AccessDetails> accessDetailsMap;
    final byte[] crlf = {'\r', '\n'};
    final byte[] crlfcrlf = {'\r', '\n', '\r', '\n'};
    final byte[] colonSpace = {':', ' '};
    final private static Queue<ByteBuffer> bufferQueue = new ConcurrentLinkedQueue<>();
    public static final int BUFFER_SIZE = 10000;
    final ExecutorService requestProcessing = Executors.newCachedThreadPool();

    public CamWebadminHostProxy(ILogService logService, ICamServiceInterface camService) {
        accessDetailsMap = new HashMap<>();
        this.logService = logService;
        this.camService = camService;
    }

    /**
     * It will run a single-threaded proxy server on
     * the provided local port.
     */
    public void runServer(int localport) {

        Executors.newSingleThreadExecutor().execute(() -> {
            // Creating a ServerSocket to listen for connections with
            try (ServerSocketChannel s = ServerSocketChannel.open()) {
                s.bind(new InetSocketAddress(localport));
                while (true) {
                    SocketChannel client;
                    try {
                        // Wait for a connection on the local port
                        client = s.accept();

                        requestProcessing(client);
                    } catch (Exception ex) {
                        logService.getCam().error(ex.getClass().getName() + " in runServer: " + ex.getMessage());
                        break;
                    }
                }
            } catch (Exception ex) {
                logService.getCam().error(ex.getClass().getName() + " in runServer (exiting thread): " + ex.getMessage());
            }
        });
    }

    void requestProcessing(@NotNull SocketChannel client) {
        requestProcessing.submit(() -> handleClientRequest(client));
    }

    private void handleClientRequest(@NotNull SocketChannel client) {
        try {
            ByteBuffer reply = CamWebadminHostProxy.getBuffer();
            final Object lock = new Object();

            // Create a connection to the real server.
            // If we cannot connect to the server, send an error to the
            // client, disconnect, and continue waiting for connections.
            try {
                SocketChannel server = SocketChannel.open();
                final AtomicReference<AccessDetails> accessDetails = new AtomicReference<>();
                final AtomicReference<ByteBuffer> updatedReq = new AtomicReference<>();
                // a thread to read the client's requests and pass them
                // to the server. A separate thread for asynchronous.
                requestProcessing.submit(() -> {
                    ByteBuffer request = CamWebadminHostProxy.getBuffer();
//                    ByteBuffer req = null;
                    try {
                        long pass = 0;

                        client.configureBlocking(true);
                        while (client.read(request) != -1) {
                            request.flip();
                            if (++pass == 1) {
                                accessDetails.set(getAccessDetails(request));
                                AccessDetails ad = accessDetails.get();
                                server.connect(new InetSocketAddress(ad.cameraHost, ad.cameraPort));
                            }

                            String x = "\nRequest before modifyHeader: " + new String(request.array(), 0, request.limit(), StandardCharsets.UTF_8);
                            logService.getCam().trace(x);
//                            AtomicReference<ByteBuffer> newReq = new AtomicReference<>();
//                            if(modifyHeader(request, newReq, "Host", accessDetails.get().cameraHost))
//                                req = newReq.get();
//                            else
//                                req = request;
                            String xyz = "\nRequest: " + new String(request.array(), 0, request.limit(), StandardCharsets.UTF_8);
                            logService.getCam().trace(xyz);
                            int bytesWritten = 0;
                            long serverPass = 0;

                            while (bytesWritten < request.limit()) {
                                if(++serverPass == 1)
                                {
                                    final String username = camService.cameraAdminUserName();
                                    final String password = camService.cameraAdminPassword();

                                    String encodedCredentials = Base64.getEncoder().encodeToString((username+":"+password).getBytes());
                                    if(addHeader(request, updatedReq, "Authorization", "Basic " + encodedCredentials)) {
                                        request = updatedReq.get();
                                        logService.getCam().trace(new String(request.array(), 0, request.limit(), StandardCharsets.UTF_8));
                                    }
                                }
                                int val = server.write(request);
                                if (val == -1)
                                    break;
                                bytesWritten += val;
                            }
                            synchronized (lock) {
                                lock.notify();
                            }
                            request.clear();
                        }
                    } catch (IOException ignore) {
                    } catch (Exception ex) {
                        logService.getCam().error(ex.getClass().getName() + " in handleClientRequest: " + ex.getMessage());
                    } finally {
                        CamWebadminHostProxy.recycle(request);
                    }
                    // the client closed the connection to us, so close our
                    // connection to the server.
                    try {
                        server.close();
                    } catch (IOException e) {
                        logService.getCam().error("IOException in handleClientRequest when closing server socket: " + e.getMessage());
                    }
                });

                try {
                    synchronized (lock) {
                        lock.wait();
                    }
                } catch (Exception ignore) {
                }

                // Read the server's responses
                // and pass them back to the client.
                try {
                    long pass = 0;
                    server.configureBlocking(true);
                    while (server.isOpen() && (server.read(reply)) != -1) {
                        reply.flip();
                        // Only set the session cookie if it's not already set
                        if (++pass == 1) {
                            if (!accessDetails.get().getHasCookie()) {
                                AtomicReference<ByteBuffer> arReply = new AtomicReference<>();

                                if (addHeader(reply, arReply, "Set-cookie", "SESSION-ID=" + accessDetails.get().getAccessToken() + "; path=/; HttpOnly"))
                                    reply = arReply.get();

                            }
                        }
                        String x = "\nReply: " + new String(reply.array(), 0, reply.limit(), StandardCharsets.UTF_8);
                        logService.getCam().trace(x);
                        client.write(reply);
                        reply.clear();
                        accessDetails.get().setHasCookie();
                    }
                } catch (IOException e) {
                    reply.flip();
                    int bytesWritten = 0;
                    while (bytesWritten < reply.limit()) {
                        int val = client.write(reply);
                        if (val == -1)
                            break;
                        bytesWritten += val;
                    }

                    logService.getCam().error("IOException in handleClientRequest: " + e.getMessage());
                }
                // The server closed its connection to us, so we close our
                // connection to our client.
                client.close();
            } catch (IOException e) {
                logService.getCam().error("IOException in handleClientRequest when opening socket channel: " + e.getMessage());
            }

            CamWebadminHostProxy.recycle(reply);

        } finally {
            try {
                client.close();
            } catch (IOException e) {
                logService.getCam().error("IOException in handleClientRequest finally block: " + e.getMessage());
            }
        }
    }

    /**
     * getAccessDetails: Check first for an accessToken in the url. If present, look up the access detail using
     * m                the token as a key.
     *
     * @param request: The request bytes from the client
     * @return Access details for the key, or null if not found.
     */
    private AccessDetails getAccessDetails(ByteBuffer request) {
        AccessDetails retVal = null;
        // Check for an access token in the URL
        String httpHeader = getHTTPHeader(request);
        final String tokenKey = "?accessToken=";
        if (httpHeader.contains(tokenKey)) {
            final int lengthOfAccessToken = 36;
            final int idx = httpHeader.indexOf(tokenKey) + tokenKey.length();
            final String accessToken = httpHeader.substring(idx, idx + lengthOfAccessToken);
            if (accessDetailsMap.containsKey(accessToken))
                retVal = accessDetailsMap.get(accessToken);
        } else {
            final String cookie = getHeader(request, "Cookie");
            final String sessionId = getSessionId(cookie);
            if (accessDetailsMap.containsKey(sessionId))
                retVal = accessDetailsMap.get(sessionId);

        }
        return retVal;
    }


    String getHeader(@NotNull ByteBuffer byteBuffer, @NotNull String key) {
        String retVal = "";
        try {
            BinarySearcher bs = new BinarySearcher();
            // Check that the double CRLF is present
            List<Integer> indexList = bs.searchBytes(byteBuffer.array(), crlfcrlf, 0, byteBuffer.limit());
            if (indexList.size() > 0) {
                // OK so look for the header key
                indexList = bs.searchBytes(byteBuffer.array(), key.getBytes(StandardCharsets.UTF_8), 0, byteBuffer.limit());
                if (indexList.size() > 0) {
                    final int idx1 = indexList.get(0);
                    // Find the CRLF at the end of this header
                    indexList = bs.searchBytes(byteBuffer.array(), crlf, idx1, byteBuffer.limit());
                    if (indexList.size() > 0) {
                        final int endIdx = indexList.get(0);
                        //Find the start of the header value
                        indexList = bs.searchBytes(byteBuffer.array(), colonSpace, idx1, endIdx);
                        if (indexList.size() == 1) {
                            final int startIdx = indexList.get(0) + colonSpace.length;
                            retVal = new String(byteBuffer.array(), startIdx, endIdx - startIdx);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logService.getCam().error(ex.getClass().getName() + " in getHeader: " + ex.getMessage());
        }
        return retVal;
    }

    String getHTTPHeader(@NotNull ByteBuffer byteBuffer) {
        String httpHeader = "";
        // Check there is a double CRLF
        BinarySearcher bs = new BinarySearcher();
        List<Integer> indexList = bs.searchBytes(byteBuffer.array(), crlfcrlf, 0, byteBuffer.limit());
        if (indexList.size() > 0) {
            // Find the first crlf
            indexList = bs.searchBytes(byteBuffer.array(), crlf, 0, byteBuffer.limit());
            if (indexList.size() > 0) {
                String firstLine = new String(byteBuffer.array(), 0, indexList.get(0));
                if (firstLine.contains("HTTP"))
                    httpHeader = firstLine;
            }
        }

        return httpHeader;
    }

    boolean addHeader(@NotNull ByteBuffer src, AtomicReference<ByteBuffer> arDest, @NotNull String key, @NotNull String value) {
        boolean retVal = false;
        final ByteBuffer srcClone = getBuffer();
        srcClone.put(src.array(), 0, src.limit());
        srcClone.flip();
        ByteBuffer dest = CamWebadminHostProxy.getBuffer();
        BinarySearcher bs = new BinarySearcher();
        // Find the first CRLF in the source buffer
        List<Integer> indexList = bs.searchBytes(srcClone.array(), crlf, 0, srcClone.limit());
        if (indexList.size() > 0) {
            final int idx1 = indexList.get(0) + crlf.length;
            // Copy up to just after the first crlf to the dest buffer
            dest.put(srcClone.array(), 0, idx1);
            // Append the new header to follow this
            dest.put(key.getBytes());
            dest.put(colonSpace);
            dest.put(value.getBytes());
            dest.put(crlf);
            // Append the remainder of the source buffer to follow this
            dest.put(srcClone.array(), idx1, srcClone.limit() - idx1);
            dest.flip();
            arDest.set(dest);
            CamWebadminHostProxy.recycle(srcClone);
            retVal = true;
        }
        return retVal;
    }

    boolean removeHeader(@NotNull ByteBuffer src, AtomicReference<ByteBuffer> arDest, @NotNull String key) {
        boolean retVal = false;
        BinarySearcher bs = new BinarySearcher();
        // Find the first CRLF in the source buffer
        List<Integer> indexList = bs.searchBytes(src.array(), key.getBytes(), 0, src.limit());
        if (indexList.size() > 0) {
            final int startIdx = indexList.get(0);
            indexList = bs.searchBytes(src.array(), crlf, startIdx, src.limit());
            if (indexList.size() > 0) {
                final int endIdx = indexList.get(0) + crlf.length;
                final ByteBuffer dest = CamWebadminHostProxy.getBuffer();
                dest.put(src.array(), 0, startIdx);
                dest.put(src.array(), endIdx, src.limit() - endIdx);
                dest.flip();
                arDest.set(dest);
                retVal = true;
            }
        }
        return retVal;
    }

    boolean modifyHeader(@NotNull ByteBuffer src, AtomicReference<ByteBuffer> arDest, @NotNull String key, @NotNull String newValue) {
        boolean retVal = false;
        final ByteBuffer srcClone = getBuffer();
        srcClone.put(src.array(), 0, src.limit());
        srcClone.flip();
        AtomicReference<ByteBuffer> headerRemoved = new AtomicReference<>();
        // First remove the existing header
        if (removeHeader(srcClone, headerRemoved, key))
            // Then add with the required new value
            retVal = addHeader(headerRemoved.get(), arDest, key, newValue);
        CamWebadminHostProxy.recycle(headerRemoved.get());
        CamWebadminHostProxy.recycle(srcClone);
        //   System.out.print(new String(headerRemoved.get().array(), 0, headerRemoved.get().limit()));
        return retVal;
    }

    String getSessionId(@NotNull String cookies) {
        String retVal = "";
        final String semiColon = ";";
        final String key = "SESSION-ID=";
        final int startIdx = cookies.indexOf(key);
        if (startIdx >= 0) {
            final int semiColonIdx = cookies.indexOf(semiColon, startIdx);
            if (semiColonIdx > 0)
                retVal = cookies.substring(startIdx + key.length(), semiColonIdx);
            else
                retVal = cookies.substring(startIdx + key.length());
        }
        return retVal;
    }

    public void addAccessToken(IGetAccessTokenCommand cmd, String accessToken) {
        AccessDetails ad = new AccessDetails(cmd.getHost(), cmd.getPort(), AccessDetails.eAuthType.basic);
        accessDetailsMap.put(accessToken, ad);
        ad.setTimer(accessToken, accessDetailsMap);
    }

    /**
     * restTimer: Is called periodically by the client to prevent the access token from timing out. When the client navigates
     * away or is closed, the access token will be removed by the timer.
     *
     * @param cmd: Contains the access token
     */
    public boolean resetTimer(IResetTimerCommand cmd) {
        synchronized (accessDetailsMap) {
            boolean retVal = true;
            if (accessDetailsMap.containsKey(cmd.getAccessToken())) {
                AccessDetails ad = accessDetailsMap.get(cmd.getAccessToken());
                ad.resetTimer();
            } else
                retVal = false;
            return retVal;
        }
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
}
