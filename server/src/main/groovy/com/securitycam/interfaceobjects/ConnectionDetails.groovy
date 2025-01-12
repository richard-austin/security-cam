package com.securitycam.interfaceobjects

class ConnectionDetails {
    ConnectionDetails(String name, String mac, String con_type, String device)
    {
        this.name = name
        this.mac = mac
        this.con_type = con_type
        this.device = device
    }

    String name
    String mac
    String con_type
    String device
}
