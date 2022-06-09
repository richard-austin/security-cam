package com.proxy;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;

public class CloudSessionTimerTask extends TimerTask {
    CloudProxy thisCP;
    private static final Logger logger = (Logger) LoggerFactory.getLogger("CLOUDPROXY");

    public CloudSessionTimerTask(CloudProxy thisCP)
    {
        this.thisCP = thisCP;
    }

    @Override
    public void run() {
        // If timer times out, then we've lost connection so try restarting the CloudProxy
        thisCP.restart();
    }
}
