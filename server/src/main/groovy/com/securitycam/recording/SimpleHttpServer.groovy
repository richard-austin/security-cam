package com.securitycam.recording

import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpExchange

class SimpleHttpServer {
    static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0)
        server.createContext("/", new MyHandler())
        server.setExecutor(null) // default executor
        server.start()
        System.out.println("Server running on port 8000")
    }

    static class MyHandler implements HttpHandler {
        @Override
        void handle(HttpExchange exchange) throws IOException {
            String response = "Hello, this is a simple HTTP server!"
            exchange.sendResponseHeaders(200, response.length())
            OutputStream os = exchange.getResponseBody()
            os.write(response.getBytes())
            os.close()
        }
    }
}
