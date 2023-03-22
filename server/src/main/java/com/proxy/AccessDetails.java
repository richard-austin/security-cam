package com.proxy;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class AccessDetails {

    Timer timer;

    public enum eAuthType {basic, other}
    String cameraHost;
    int cameraPort;
    eAuthType authType;
    final long maxTime = 36000;

    AccessDetails(String cameraHost, int cameraPort, eAuthType authType) {
        this.cameraHost = cameraHost;
        this.cameraPort = cameraPort;
        this.authType = authType;
        timer = new Timer();
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


