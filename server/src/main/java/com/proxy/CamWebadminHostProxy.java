package com.proxy;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

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
            ByteBuffer reply = ByteBuffer.allocate(4096);
            // Create a connection to the real server.
            // If we cannot connect to the server, send an error to the
            // client, disconnect, and continue waiting for connections.
            try (SocketChannel server = SocketChannel.open()) {
                server.connect(new InetSocketAddress(host, remotePort));
                final AtomicReference<String> currentSessionId = new AtomicReference<>();
                final String newSessionId = "lodfidfhwerrofgifhrfgkjfgbgkjp";
                // a thread to read the client's requests and pass them
                // to the server. A separate thread for asynchronous.
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        int pass = 0;
                        client.configureBlocking(true);
                        while (client.read(request) != -1) {
                            request.flip();
                            if (++pass == 1) {
                                String cookie = getHeader(request, "Cookie");
                                currentSessionId.set(getSessionId(cookie));
                             }
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
                    int pass = 0;
                    server.configureBlocking(true);
                    while (server.isOpen() && (server.read(reply)) != -1) {
                        reply.flip();
                        // Only set the session cookie if it's not already set
                        if (++pass == 1) {
                            if(!Objects.equals(currentSessionId.get(), newSessionId)) {
                                AtomicReference<ByteBuffer> arReply = new AtomicReference<>();
                                if (addHeader(reply, arReply, "Set-cookie", "SESSION-ID=" + newSessionId+"; path=/"))
                                    reply = arReply.get();
                            }
                        }
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

    String getHeader(@NotNull ByteBuffer byteBuffer, @NotNull String key) {
        final byte[] crlfcrlf = {'\r', '\n', '\r', '\n'};
        final byte[] crlf = {'\r', '\n'};
        final byte[] colonSpace = {':', ' '};
        String retVal = "";
        try {
            BinarySearcher bs = new BinarySearcher();
            // Check that the double CRLF is present
            List<Integer> indexList = bs.searchBytes(byteBuffer.array(), crlfcrlf);
            if (indexList.size() > 0) {
                // OK so look for the header key
                indexList = bs.searchBytes(byteBuffer.array(), key.getBytes(StandardCharsets.UTF_8));
                if (indexList.size() > 0) {
                    final int idx1 = indexList.get(0);
                    // Find the CRLF at the end of this header
                    indexList = bs.searchBytes(byteBuffer.array(), crlf, idx1);
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
        final byte[] crlfcrlf = {'\r', '\n', '\r', '\n'};
        final byte[] crlf = {'\r', '\n'};
        // Check there is a double CRLF
        BinarySearcher bs = new BinarySearcher();
        List<Integer> indexList = bs.searchBytes(byteBuffer.array(), crlfcrlf);
        if(indexList.size()>0) {
            // Find the first crlf
            indexList = bs.searchBytes(byteBuffer.array(), crlf);
            if(indexList.size() > 0)
            {
                String firstLine = new String(byteBuffer.array(), 0, indexList.get(0));
                if(firstLine.contains("HTTP"))
                    httpHeader = firstLine;
            }
        }

        return  httpHeader;
    }

    boolean addHeader(@NotNull ByteBuffer src, AtomicReference<ByteBuffer> arDest, @NotNull String key, @NotNull String value) {
        final byte[] crlf = {'\r', '\n'};
        final byte[] colonSpace = {':', ' '};
        boolean retVal = false;
        ByteBuffer dest = ByteBuffer.allocate(src.limit() + key.length() + colonSpace.length + value.length() + crlf.length);
        BinarySearcher bs = new BinarySearcher();
        // Find the first CRLF in the source buffer
        List<Integer> indexList = bs.searchBytes(src.array(), crlf);
        if (indexList.size() > 0) {
            final int idx1 = indexList.get(0) + crlf.length;
            // Copy up to just after the first crlf to the dest buffer
            dest.put(src.array(), 0, idx1);
            // Append the new header to follow this
            dest.put(key.getBytes());
            dest.put(colonSpace);
            dest.put(value.getBytes());
            dest.put(crlf);
            // Append the remainder of the source buffer to follow this
            dest.put(src.array(), idx1, src.limit() - idx1);
            dest.flip();
            arDest.set(dest);
            retVal = true;
        }
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
}
