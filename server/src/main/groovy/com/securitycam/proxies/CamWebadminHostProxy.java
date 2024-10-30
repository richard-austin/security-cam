package com.securitycam.proxies;

import com.proxy.ILogService;
import com.securitycam.controllers.Camera;
import com.securitycam.controllers.CameraAdminCredentials;
import com.securitycam.interfaceobjects.AccessDetails;
import com.securitycam.services.CamService;
import common.HeaderProcessing;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CamWebadminHostProxy extends HeaderProcessing {

    // Camera types
    final int none = 0;
    final int sv3c = 1;
    final int zxtechMCW5B10X = 2;

    ILogService logService;
    CamService camService;
    final Map<String, AccessDetails> accessDetailsMap;
    final ExecutorService processRequestThread = Executors.newCachedThreadPool();

    public CamWebadminHostProxy(ILogService logService, CamService camService) {
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
                        handleClientRequest(client);
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

    private void handleClientRequest(SocketChannel client) {
        processRequestThread.submit(() -> {
            try {
                ByteBuffer reply = getBuffer(false);
                final Object lock = new Object();
                // Create a connection to the real server.
                // If we cannot connect to the server, send an error to the
                // client, disconnect, and continue waiting for connections.
                try {
                    SocketChannel server = SocketChannel.open();
                    server.socket().setReceiveBufferSize(BUFFER_SIZE);
                    //server.configureBlocking(true);
                    final AtomicReference<AccessDetails> accessDetails = new AtomicReference<>();
                    final AtomicReference<ByteBuffer> updatedReq = new AtomicReference<>();
                    final AtomicInteger camType = new AtomicInteger();
                    // a thread to read the client's requests and pass them
                    // to the server. A separate thread for asynchronous.
                    processRequestThread.submit(() -> {
                        ByteBuffer request = getBuffer(false);
                        try {
                            long pass = 0;

                            client.configureBlocking(true);
                            logService.getCam().trace("handleClientRequest: Ready to read client request");
                            while (client.read(request) != -1) {
                                request.flip();
                                AccessDetails ad = null;
                                if (++pass == 1) {
                                    accessDetails.set(getAccessDetails(request));
                                    ad = accessDetails.get();
                                    if (ad != null) {
                                        ad.addClient(client);  // Add to the list for forced close on exit from hosting
                                        Integer ct = camService.getCameraType(ad.cameraHost);
                                        camType.set(ct);
                                        server.connect(new InetSocketAddress(ad.cameraHost, ad.cameraPort));
                                    } else
                                        logService.getCam().error("No accessToken found for request");
                                    AtomicReference<ByteBuffer> newReq = new AtomicReference<>();
                                    if (modifyHeader(request, newReq, "Host", accessDetails.get().cameraHost)) {
                                        request = newReq.get();
                                    }
                                }
                                logService.getCam().trace("pass = " + pass);
                                int bytesWritten = 0;
                                long serverPass = 0;

                                while (bytesWritten < request.limit()) {
                                    //Only mess with headers on the first pass
                                    if (++serverPass == 1) {
                                        // Camera types sv3c and zxtech use basic auth, only apply to these
                                        if (camType.get() == sv3c || camType.get() == zxtechMCW5B10X) {
                                            assert ad != null;
                                            Camera cam = camService.getCamera(ad.cameraHost);
                                            if(cam == null)
                                                throw new Exception("Couldn't find a camera with the address "+ad.cameraHost);
                                            CameraAdminCredentials creds = cam.credentials();
                                            final String username = creds.getUserName();
                                            final String password = creds.getPassword();
                                            String encodedCredentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
                                            if (addHeader(request, updatedReq, "Authorization", "Basic " + encodedCredentials)) {
                                                request = updatedReq.get();
                                            }
                                        }

                                        logService.getCam().trace("serverPass = " + serverPass);
                                    }
                                    // String xyz = "\nRequest: " + new String(request.array(), 0, request.limit(), StandardCharsets.UTF_8);
                                    // logService.getCam().trace(xyz);
                                    int val = server.write(request);
                                    if (serverPass == 1) {
                                        synchronized (lock) {
                                            lock.notify();
                                        }
                                    }
                                    if (val == -1)
                                        break;
                                    bytesWritten += val;
                                }
                                request.clear();
                            }
                            server.close();
                            logService.getCam().trace("handleClientRequest: Out of client request loop");
                        } catch (IOException ignore) {
                        } catch (Exception ex) {
                            logService.getCam().error(ex.getClass().getName() + " in handleClientRequest: " + ex.getMessage());
                        } finally {
                            recycle(request);
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
                        logService.getCam().trace("handleClientRequest: Ready to read device response");
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
                            logService.getCam().trace("server.read pass = " + pass);
                            // String x = "\nReply: " + new String(reply.array(), 0, reply.limit(), StandardCharsets.UTF_8);
                            // logService.getCam().trace(x);
                            client.write(reply);
                            accessDetails.get().setHasCookie();
                            reply.clear();
                        }
                        server.close();
                        logService.getCam().trace("\"handleClientRequest: Out of device response loop");
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

                        logService.getCam().error(e.getClass().getName() + " in handleClientRequest 1: " + e.getMessage());
                    }
                    // The server closed its connection to us, so we close our
                    // connection to our client.
                    client.close();
                } catch (Exception ex) {
                    logService.getCam().error(ex.getClass().getName() + " in handleClientRequest (inner) when opening socket channel: " + ex.getMessage());
                }
                recycle(reply);

            } catch (Exception ex) {
                logService.getCam().error(ex.getClass().getName() + " in handleClientRequest (outer) when opening socket channel: " + ex.getMessage());
            } finally {
                try {
                    client.close();
                } catch (IOException e) {
                    logService.getCam().error("IOException in handleClientRequest finally block: " + e.getMessage());
                }
            }
        });
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
