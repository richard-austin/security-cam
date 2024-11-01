package com.securitycam.interfaceobjects

class CurrentWifiConnection {
    CurrentWifiConnection(String accessPoint, String[] lines)
    {
        this.accessPoint = accessPoint
        this.lines = lines
    }

    String accessPoint
    String[] lines
}
