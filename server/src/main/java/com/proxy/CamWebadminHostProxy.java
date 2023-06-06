package com.proxy;

import common.HeaderProcessing;
//import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CamWebadminHostProxy extends HeaderProcessing {

    // Camera types
    final int none = 0;
    final int sv3c = 1;
    final int zxtechMCW5B10X =2;

    ILogService logService;
    ICamServiceInterface camService;
    final Map<String, AccessDetails> accessDetailsMap;
    final ExecutorService requestProcessing = Executors.newCachedThreadPool();

    public CamWebadminHostProxy(ILogService logService, ICamServiceInterface camService) {
        super(logService);
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

    void requestProcessing(SocketChannel client) {
        requestProcessing.submit(() -> handleClientRequest(client));
    }

    private void handleClientRequest(SocketChannel client) {
        try {
            ByteBuffer reply = getBuffer();
            final Object lock = new Object();

            // Create a connection to the real server.
            // If we cannot connect to the server, send an error to the
            // client, disconnect, and continue waiting for connections.
            try {
                SocketChannel server = SocketChannel.open();
                final AtomicReference<AccessDetails> accessDetails = new AtomicReference<>();
                final AtomicReference<ByteBuffer> updatedReq = new AtomicReference<>();
                final AtomicInteger camType = new AtomicInteger();
                // a thread to read the client's requests and pass them
                // to the server. A separate thread for asynchronous.
                requestProcessing.submit(() -> {
                    ByteBuffer request = getBuffer();
                    try {
                        long pass = 0;

                        client.configureBlocking(true);
                        while (client.read(request) != -1) {
                            request.flip();
                            if (++pass == 1) {
                                accessDetails.set(getAccessDetails(request));
                                AccessDetails ad = accessDetails.get();
                                if(ad != null) {
                                    ad.addClient(client);  // Add to the list for forced close on exit from hosting
                                    Integer ct = camService.getCameraType(ad.cameraHost);
                                    camType.set(ct);
                                    server.connect(new InetSocketAddress(ad.cameraHost, ad.cameraPort));
                                }
                            }

                            AtomicReference<ByteBuffer> newReq = new AtomicReference<>();
                            if(modifyHeader(request, newReq, "Host", accessDetails.get().cameraHost)) {
                                request = newReq.get();
                            }
                            int bytesWritten = 0;
                            long serverPass = 0;

                            while (bytesWritten < request.limit()) {
                                //Only mess with headers on the first pass
                                if (++serverPass == 1) {
                                    // Camera types sv3c and zxtech use basic auth, only apply to these
                                    if(camType.get() == sv3c || camType.get() == zxtechMCW5B10X) {
                                        final String username = camService.cameraAdminUserName();
                                        final String password = camService.cameraAdminPassword();

                                        String encodedCredentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
                                        if (addHeader(request, updatedReq, "Authorization", "Basic " + encodedCredentials)) {
                                            request = updatedReq.get();
                                        }
                                    }
                                }
                                String xyz = "\nRequest: " + new String(request.array(), 0, request.limit(), StandardCharsets.UTF_8);
                                logService.getCam().trace(xyz);
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
                        recycle(request);
                    }
                    // the client closed the connection to us, so close our
                    // connection to the server.
                    try {
                        server.close();
                    } catch (IOException e) {
                        logService.getCam().error(e.getClass().getName()+" in handleClientRequest when closing server socket: " + e.getMessage());
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
                        AtomicReference<ByteBuffer> arNoXFrame = new AtomicReference<>();
                        if(camType.get() == none) {
                            if (removeHeader(reply, arNoXFrame, "X-Frame-Options"))
                                reply = arNoXFrame.get();
                            if (removeHeader(reply, arNoXFrame, "X-Xss-Protection"))
                                reply = arNoXFrame.get();
                       }
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
                } catch (ClosedChannelException ignore) {
                } catch (IOException e) {
                    reply.flip();
                    int bytesWritten = 0;
                    while (bytesWritten < reply.limit()) {
                        int val = client.write(reply);
                        if (val == -1)
                            break;
                        bytesWritten += val;
                    }

                    logService.getCam().error(e.getClass().getName()+" in handleClientRequest 1: " + e.getMessage());
                }
                // The server closed its connection to us, so we close our
                // connection to our client.
                client.close();
            } catch (IOException e) {
                logService.getCam().error(e.getClass().getName()+" in handleClientRequest when opening socket channel: " + e.getMessage());
            }

            recycle(reply);

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

    String getSessionId(String cookies) {
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

    public boolean closeClientConnections(String accessToken) {
        synchronized (accessDetailsMap) {
            boolean retVal = true;
            AccessDetails ad = accessDetailsMap.get(accessToken);
            if (ad != null)
                ad.closeClients();
            else
                retVal = false;

            return retVal;
        }
    }
}
