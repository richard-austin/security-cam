package com.securitycam.timers

import com.securitycam.services.CloudProxyService


class CloudProxyRestartTask extends TimerTask {
    CloudProxyRestartTask(CloudProxyService cloudProxyService)
    {
        this.cloudProxyService = cloudProxyService
    }
    final Object synchObject = new Object()
    CloudProxyService cloudProxyService
    @Override
    void run() {
        try {

            synchronized (synchObject) {
                cloudProxyService.stop()
                synchObject.wait(3000)
                cloudProxyService.start()
            }
        }
        catch(Exception ignored)
        {
        }
    }
}
