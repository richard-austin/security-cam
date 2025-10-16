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
    AccessDetails accessDetails;
    final ExecutorService processRequestThread = Executors.newCachedThreadPool();

    public CamWebadminHostProxy(ILogService logService, CamService camService) {
        super(logService);
        accessDetails = null;
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
                            AccessDetails ad = null;
                            while (client.read(request) != -1) {
                                request.flip();

                                String x = getHTTPHeader(request);
                                logService.getCam().info(x);


                                if (++pass == 1) {
                                    accessDetails.set(getAccessDetails());
                                    ad = accessDetails.get();
                                    if (ad != null) {
                                        ad.addClient(client);  // Add to the list for forced close on exit from hosting
                                        Integer ct = camService.getCameraType(ad.cameraHost);
                                        camType.set(ct);
                                        server.connect(new InetSocketAddress(ad.cameraHost, ad.cameraPort));
                                        AtomicReference<ByteBuffer> newReq = new AtomicReference<>();
                                        if (modifyHeader(request, newReq, "Host", accessDetails.get().cameraHost)) {
                                            request = newReq.get();
                                        }
                                        if (addHeader(request, newReq, "Cache-Control", "'no-store, no-cache, must-revalidate")) {
                                            request = newReq.get();
                                        }
                                    } else
                                        logService.getCam().error("No accessToken found for request");
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
                                            if (cam == null)
                                                throw new Exception("Couldn't find a camera with the address " + ad.cameraHost);
                                            CameraAdminCredentials creds = cam.credentials();
                                            final String username = creds.getUserName();
                                            final String password = creds.getPassword();
                                            String encodedCredentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
                                            if (addHeader(request, updatedReq, "Authorization", "Basic " + encodedCredentials)) {
                                                request = updatedReq.get();
                                            }
                                        }
                                    }
                                    logService.getCam().trace("serverPass = {}", serverPass);
                                    // String xyz = "\nRequest: " + new String(request.array(), 0, request.limit(), StandardCharsets.UTF_8);
                                    // logService.getCam().trace(xyz);
                                    int val = server.write(request);
                                    if (serverPass == 1) {
                                        synchronized (lock) {
                                            lock.notify();
                                            logService.getCam().info("notified!!!");
                                        }
                                    }
                                    if (val == -1)
                                        break;
                                    bytesWritten += val;
                                }
                                request.clear();
                            }
                            server.close();
                            //client.close();
                            logService.getCam().trace("handleClientRequest: Out of client request loop");
                        } catch (IOException ignore) {
                        } catch (Exception ex) {
                            logService.getCam().error("{} in handleClientRequest: {}", ex.getClass().getName(), ex.getMessage());
                        } finally {
                            recycle(request);
                        }
                    });

                    try {
                        synchronized (lock) {
                            lock.wait();
                            logService.getCam().info("Request written to server");
                        }
                    } catch (Exception ignore) {
                    }

                    // Read the server's responses
                    // and pass them back to the client.
                    try {
                        logService.getCam().info("handleClientRequest: Ready to read device response");
                        var pass = 0;
                        while (server.isOpen() && server.read(reply) != -1) {
                            logService.getCam().info("Server response: {}", ++pass);
                            reply.flip();
                            // String x = "\nReply: " + new String(reply.array(), 0, reply.limit(), StandardCharsets.UTF_8);
                            // logService.getCam().trace(x);
                            if (client.isOpen() && reply.limit() > 0) {
                                client.write(reply);
                            }
                            reply.clear();
                        }
                        server.close();
                        client.close();
                        logService.getCam().info("\"handleClientRequest: Out of device response loop");
                    } catch (ClosedChannelException ignore) {
                        server.close();
                        client.close();
                    } catch (IOException e) {
                        server.close();
                        client.close();
                        logService.getCam().error("{} in handleClientRequest 1: {}", e.getClass().getName(), e.getMessage());
                    }
                    // The server closed its connection to us, so we close our
                    // connection to our client.
                    client.close();
                } catch (Exception ex) {
                    logService.getCam().error("{} in handleClientRequest (inner) when opening socket channel: {}", ex.getClass().getName(), ex.getMessage());
                }
                recycle(reply);

            } catch (Exception ex) {
                logService.getCam().error("{} in handleClientRequest (outer) when opening socket channel: {}", ex.getClass().getName(), ex.getMessage());
            } finally {
                try {
                    client.close();
                } catch (IOException e) {
                    logService.getCam().error("IOException in handleClientRequest finally block: {}", e.getMessage());
                }
            }
        });
    }

    /**
     * getAccessDetails: Check first for an accessToken in the url. If present, look up the access detail using
     * m                the token as a key.
     *
     * @return Access details for the key, or null if not found.
     */
    private AccessDetails getAccessDetails() {
        return accessDetails;
    }

    public void enableAccess(IGetHostingAccessCommand cmd) {
        AccessDetails ad = new AccessDetails(cmd.getHost(), cmd.getPort(), AccessDetails.eAuthType.basic);
        accessDetails = ad;
        ad.setTimer();
    }

    /**
     * restTimer: Is called periodically by the client to prevent the access token from timing out. When the client navigates
     * away or is closed, the access token will be removed by the timer.
     *
     */
    public boolean resetTimer() {
        boolean retVal = true;
        if (accessDetails != null) {
            accessDetails.resetTimer();
        } else
            retVal = false;
        return retVal;
    }

    public boolean closeClientConnection() {
        boolean retVal = true;
        if (accessDetails != null)
            accessDetails.closeClients();
        else
            retVal = false;

        return retVal;
    }
}
