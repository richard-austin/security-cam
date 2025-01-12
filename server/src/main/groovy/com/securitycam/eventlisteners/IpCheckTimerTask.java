package com.securitycam.eventlisteners;

import java.util.TimerTask;

public class IpCheckTimerTask extends TimerTask {

    ipCheckIF ipChkCB;

    public IpCheckTimerTask(ipCheckIF ipChkCB)
    {
        this.ipChkCB = ipChkCB;
    }

    @Override
    public void run() {
        ipChkCB.ipCheck();
    }
}
