package com.securitycam.interfaceobjects;

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
    public String cameraHost;
    public int cameraPort;
    eAuthType authType;
    final long maxTime = 36000;
    boolean hasCookie;

    List<SocketChannel> activeClients;

    public AccessDetails(String cameraHost, int cameraPort, eAuthType authType) {
        this.cameraHost = cameraHost;
        this.cameraPort = cameraPort;
        this.authType = authType;
        timer = new Timer();
        hasCookie = false;
        activeClients = new ArrayList<>();
    }

    private String accessToken;
    Map<String, AccessDetails> map;

    public void setTimer(String accessToken, Map<String, AccessDetails> map) {
        this.accessToken = accessToken;
        this.map = map;
        timer.schedule(new RemoveAccessTokenTask(accessToken, map), maxTime);
    }

    public void resetTimer() {
        timer.cancel();
        timer = new Timer();
        timer.schedule(new RemoveAccessTokenTask(accessToken, map), maxTime);
    }

    public void setHasCookie() {
        hasCookie = true;
    }

    public boolean getHasCookie() {
        return hasCookie;
    }

    public String getAccessToken() {
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


