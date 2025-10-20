package com.securitycam.interfaceobjects;

import java.nio.channels.SocketChannel;
import java.util.*;

public class AccessDetails {

    public void addClient(SocketChannel client) {
        activeClients.add(client);
    }

    public void closeClients() {
        activeClients.forEach((client) -> {
            try {
                if (client.isConnected()) {
                    client.shutdownInput();
                    client.shutdownOutput();
                    if (client.isOpen())
                        client.close();
                }
            } catch (Exception ignore) {
            }
        });
        activeClients.clear();
    }

    public enum eAuthType {basic, other}

    public String cameraHost;
    public int cameraPort;
    eAuthType authType;
    List<SocketChannel> activeClients;

    public AccessDetails(String cameraHost, int cameraPort, eAuthType authType) {
        this.cameraHost = cameraHost;
        this.cameraPort = cameraPort;
        this.authType = authType;
        activeClients = new ArrayList<>();
    }

}
