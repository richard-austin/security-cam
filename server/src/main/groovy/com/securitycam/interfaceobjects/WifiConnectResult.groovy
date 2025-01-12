package com.securitycam.interfaceobjects

class WifiConnectResult {
    String message
    int errorCode

    WifiConnectResult(int returnCode, String message)
    {
        this.message = message
        errorCode = returnCode
    }
}
