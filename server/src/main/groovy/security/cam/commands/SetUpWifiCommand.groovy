package security.cam.commands

import grails.core.GrailsApplication
import grails.validation.Validateable
import org.apache.commons.lang.StringUtils
import security.cam.UtilsService
import security.cam.interfaceobjects.ConnectionDetails

class SetUpWifiCommand implements Validateable {
    String ssid
    String password

    UtilsService utilsService
    GrailsApplication grailsApplication

    static constraints = {
        ssid(nullable: false, blank: false,
                validator: { ssid, cmd ->
                    // We need to check that the cloud proxy is connecting through ethernet before changing WiFi
                    if(!cmd.checkConnectedThroughEthernet())
                        return "Not connected to Cloud through Ethernet"
                })
        password(nullable: false, blank: false)
    }

    private boolean checkConnectedThroughEthernet() {
        String connections = utilsService.executeLinuxCommand("nmcli -t con show")
        String[] lines = connections.split("\n")
        ArrayList<ConnectionDetails> cdList = new ArrayList<>()
        for (line in lines) {
            String[] result = line.split(':')
            if (result.length == 4) {
                ConnectionDetails connection_details = new ConnectionDetails(result[0],
                        result[1].replace('\\', ''),
                        result[2],
                        result[3])
                cdList.add(connection_details)
            }
        }

        ConnectionDetails ethernetCon = cdList.find(cd -> {
            return cd.con_type.contains("ethernet")
        })

        // Check if there is an Ethernet connection
        if(ethernetCon == null)
            return "An active Ethernet connection is required to set up wifi"
        String[] command = [
            "/bin/sh",
            "-c",
            "ip addr show ${ethernetCon.getDevice()} | grep -w inet"
        ]

        // Find the IP address for the Ethernet interface
        String ipAddrShowOutput = utilsService.executeLinuxCommand(command)
        def ipAddress = StringUtils.substringBetween(ipAddrShowOutput, "inet ", "/")
        Integer cloudPort = (Integer)(grailsApplication.config.cloudProxy.cloudPort)
        command = [
                "/bin/sh",
                "-c",
                "ss -n | grep ${ipAddress} | grep ${cloudPort}"
        ]

        String cloudEtherConn = utilsService.executeLinuxCommand(command)
        return cloudEtherConn != ""
    }
}
