package com.securitycam.commands

class SetUpWifiCommand {

    SetUpWifiCommand() {

    }

    String ssid

    String password

    Boolean isEnabled

    String getSsid() {
        return ssid;
    }

    String getPassword() {
        return password;
    }

    boolean GetIsEnabled() {
        return isEnabled;
    }
 }
