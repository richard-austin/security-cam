package security.cam.interfaceobjects

import org.apache.commons.lang.StringUtils

class WifiConnectResult {
    String message
    int errorCode

    WifiConnectResult(int returnCode, String message)
    {
        this.message = message
        errorCode = returnCode
    }
}
