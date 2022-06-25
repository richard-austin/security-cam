package security.cam.interfaceobjects

import org.apache.commons.lang.StringUtils

class WifiConnectResult {
    WifiConnectResult(String message)
    {
        this.message = message

        // Find the error code (if any)
        String strErrCode = StringUtils.substringBetween(message, ": (", ")")
        if(strErrCode != null)
            errorCode = Integer.parseInt(strErrCode)
        else
            errorCode = 0
    }
    String message
    int errorCode = 0
}
