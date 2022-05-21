package com.proxy;

import java.util.TimerTask;

public class CloudSessionTimerTask extends TimerTask {
    CloudProxy thisCP;
    CloudSessionTimerTask(CloudProxy thisCP)
    {
        this.thisCP = thisCP;
    }

    @Override
    public void run() {
        // If timer times out, then we've lost connection so try restarting the CloudProxy
        thisCP.restart();
    }
}
