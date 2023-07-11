package com.proxy;

import java.nio.channels.SocketChannel;
import java.util.*;

public class AccessDetails {

    Timer timer;

    public void addClient(SocketChannel client) {
        activeClients.add(client);
    }

    public void closeClients() {
        activeClients.forEach((client) -> {
            try {
                if(client.isConnected()) {
                    client.shutdownInput();
                    client.shutdownOutput();
                    if (client.isOpen())
                        client.close();
                }
           }
            catch(Exception ignore) {}
        });
        activeClients.clear();
    }

    public enum eAuthType {basic, other}
    String cameraHost;
    int cameraPort;
    eAuthType authType;
    final long maxTime = 36000;
    boolean hasCookie;

    List<SocketChannel> activeClients;

    AccessDetails(String cameraHost, int cameraPort, eAuthType authType) {
        this.cameraHost = cameraHost;
        this.cameraPort = cameraPort;
        this.authType = authType;
        timer = new Timer();
        hasCookie = false;
        activeClients = new ArrayList<>();
    }

    String accessToken;
    Map<String, AccessDetails> map;

    void setTimer(String accessToken, Map<String, AccessDetails> map) {
        this.accessToken = accessToken;
        this.map = map;
        timer.schedule(new RemoveAccessTokenTask(accessToken, map), maxTime);
    }

    void resetTimer() {
        timer.cancel();
        timer = new Timer();
        timer.schedule(new RemoveAccessTokenTask(accessToken, map), maxTime);
    }

    void setHasCookie() {
        hasCookie = true;
    }

    boolean getHasCookie() {
        return hasCookie;
    }

    String getAccessToken() {
        return accessToken;
    }
}


class RemoveAccessTokenTask extends TimerTask {
    final String accessToken;
    final Map<String, AccessDetails> map;
    RemoveAccessTokenTask(String accessToken, Map<String, AccessDetails> map) {
        this.accessToken = accessToken;
        this.map = map;
    }
    @Override
    public void run() {
        synchronized (map) {

            AccessDetails ad = map.get(accessToken);
            ad.timer.purge();

            map.remove(accessToken);
        }
    }
}


