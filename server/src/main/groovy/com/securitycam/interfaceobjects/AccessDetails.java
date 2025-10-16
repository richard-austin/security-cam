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
    final long maxTime = 36000;

    List<SocketChannel> activeClients;

    public AccessDetails(String cameraHost, int cameraPort, eAuthType authType) {
        this.cameraHost = cameraHost;
        this.cameraPort = cameraPort;
        this.authType = authType;
        timer = new Timer();
        activeClients = new ArrayList<>();
    }

    public void setTimer() {
        timer.schedule(new RemoveAccessTokenTask(this), maxTime);
    }

    public void resetTimer() {
        timer.cancel();
        timer = new Timer();
        timer.schedule(new RemoveAccessTokenTask(this), maxTime);
    }

    public void purgeTimer() {
        timer.purge();
        timer = null;
    }
}


class RemoveAccessTokenTask extends TimerTask {
    AccessDetails accessDetails;

    RemoveAccessTokenTask(final AccessDetails accessDetails) {
        this.accessDetails = accessDetails;
    }

    @Override
    public void run() {
        synchronized (this) {
            if(accessDetails.timer != null)
                accessDetails.timer.purge();
            accessDetails.closeClients();
            accessDetails.activeClients.clear();
            accessDetails.cameraHost = null;
            accessDetails.cameraPort = 0;
            accessDetails.authType = AccessDetails.eAuthType.other;
        }
    }
}


