package com.securitycam.commands

class PtzCommands {
    String creds

    // These are not Restful; API parameters, only creds (encrypted) form is sent from the client.
    //  these are populated by the validator
    String user, password

}
