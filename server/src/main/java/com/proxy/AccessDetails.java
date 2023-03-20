package com.proxy;

public class AccessDetails {

    AccessDetails(String cameraHost, int cameraPort, eAuthType authType) {
        this.cameraHost = cameraHost;
        this.cameraPort = cameraPort;
        this.authType = authType;
    }
    public enum eAuthType {basic, other}
    String cameraHost;
    int cameraPort;
    eAuthType authType;
}
