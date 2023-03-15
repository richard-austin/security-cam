package com.proxy;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class CamWebadminHostProxy {
    ILogService logService;

    public CamWebadminHostProxy(ILogService logService) {
        this.logService = logService;
    }

     /**
     * It will run a single-threaded proxy server on
     * the provided local port.
     */
    public void runServer(String host, int remotePort, int localport)
            throws IOException {
        // Creating a ServerSocket to listen for connections with
        try (ServerSocketChannel s = ServerSocketChannel.open()) {
            s.bind(new InetSocketAddress(localport));
            while (true) {
                SocketChannel client;
                try {
                    // Wait for a connection on the local port
                    client = s.accept();

                    requestProcessing(client, host, remotePort);
                } catch (Exception ex) {
                    logService.getCam().error(ex.getClass().getName() + " in runServer: " + ex.getMessage());
                    break;
                }
            }
        }
    }

    void requestProcessing(@NotNull SocketChannel client, String host, int remotePort) {
        Executors.newSingleThreadExecutor().execute(() -> handleClientRequest(client, host, remotePort));
    }

    private void handleClientRequest(@NotNull SocketChannel client, String host, int remotePort) {
        try {
            final ByteBuffer request = ByteBuffer.allocate(1024);
            final ByteBuffer reply = ByteBuffer.allocate(4096);
            // Create a connection to the real server.
            // If we cannot connect to the server, send an error to the
            // client, disconnect, and continue waiting for connections.
            try (SocketChannel server = SocketChannel.open()) {
                server.connect(new InetSocketAddress(host, remotePort));

                // a thread to read the client's requests and pass them
                // to the server. A separate thread for asynchronous.
                Executors.newSingleThreadExecutor().execute(() ->  {
                    try {
                        client.configureBlocking(true);
                        while (client.read(request) != -1) {
                            request.flip();
                            String x = "Request: " + new String(request.array(), StandardCharsets.UTF_8);
                            logService.getCam().trace(x);
                            int bytesWritten = 0;
                            while (bytesWritten < request.limit()) {
                                int val = server.write(request);
                                if (val == -1)
                                    break;
                                bytesWritten += val;
                            }
                            request.clear();
                            // s.flush();
                        }
                    } catch (IOException e) {
                        request.flip();
                        try {
                            server.write(request);
                        } catch (Exception ex) {
                            logService.getCam().error(ex.getClass().getName() + " in handleClientRequest when writing request: " + ex.getMessage());
                        }
                        logService.getCam().error("IOException in handleClientRequest when in write request loop: " + e.getMessage());
                    }
                    // the client closed the connection to us, so close our
                    // connection to the server.
                    try {
                        server.close();
                    } catch (IOException e) {
                        logService.getCam().error("IOException in handleClientRequest when closing server socket: " + e.getMessage());
                    }
                });

                // Read the server's responses
                // and pass them back to the client.
                try {
                    server.configureBlocking(true);
                    while (server.isOpen() && (server.read(reply)) != -1) {
                        reply.flip();
                        String x = "Reply: " + new String(reply.array(), StandardCharsets.UTF_8);
                        logService.getCam().trace(x);
                        client.write(reply);
                        reply.clear();
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
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                logService.getCam().error("IOException in handleClientRequest finally block: " + e.getMessage());
            }
        }
    }
}
