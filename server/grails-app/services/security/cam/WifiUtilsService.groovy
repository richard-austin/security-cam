package security.cam

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import org.apache.commons.lang.StringUtils
import security.cam.commands.SetUpWifiCommand
import security.cam.commands.SetWifiStatusCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ConnectionDetails
import security.cam.interfaceobjects.EthernetStatusEnum
import security.cam.interfaceobjects.ObjectCommandResponse
import security.cam.interfaceobjects.RestfulResponse

@Transactional
class WifiUtilsService {
    LogService logService
    UtilsService utilsService
    GrailsApplication grailsApplication
    RestfulInterfaceService restfulInterfaceService

    def scanWifi() {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            RestfulResponse resp =
                    restfulInterfaceService.sendRequest("localhost:8000", "/",
                            "{\"command\": \"scanwifi\"}",
                            true, 60)

            result.responseObject = resp

        }
        catch (Exception ex) {
            logService.cam.error("Exception in scanWifi: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }

        return result
    }

    def setUpWifi(SetUpWifiCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            EthernetStatusEnum status = checkConnectedThroughEthernet()

            if (status == EthernetStatusEnum.connectedViaEthernet) {
                RestfulResponse resp =
                        restfulInterfaceService.sendRequest("localhost:8000", "/",
                                "{\"command\": \"setupwifi\", \"ssid\": \"${cmd.ssid}\", \"password\": \"${cmd.password}\"}",

                                true, 60)
                result.responseObject = resp
            } else {
                result.status = PassFail.FAIL
                result.error = "Must be connected via Ethernet to set up a Wifi Connection"
            }
        }
        catch (Exception ex) {
            logService.cam.error("Exception in setUpWifi: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }

        return result
    }

    def checkWifiStatus() {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            RestfulResponse resp =
                    restfulInterfaceService.sendRequest("localhost:8000", "/",
                            "{\"command\": \"checkwifistatus\"}",
                            true, 60)
            result.responseObject = resp
        }
        catch (Exception ex) {
            logService.cam.error("Exception in checkWifiStatus: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    def setWifiStatus(SetWifiStatusCommand cmd) {
        ObjectCommandResponse result = new ObjectCommandResponse()

        try {
            EthernetStatusEnum status = checkConnectedThroughEthernet()

            if (cmd.status != "off" || status == EthernetStatusEnum.connectedViaEthernet) {
                RestfulResponse resp =
                        restfulInterfaceService.sendRequest("localhost:8000", "/",
                                "{\"command\": \"setwifistatus\", \"status\": \"" + cmd.status + "\"}",
                                true, 60)
                result.responseObject = resp
            } else {
                result.status = PassFail.FAIL
                result.error = "Must be connected via Ethernet to set Wifi status to off"
            }
        }
        catch (Exception ex) {
            logService.cam.error("Exception in setWifiStatus: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    private EthernetStatusEnum checkConnectedThroughEthernet() {
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
        if (ethernetCon == null)
            return EthernetStatusEnum.noEthernet
        String[] command = [
                "/bin/sh",
                "-c",
                "ip addr show ${ethernetCon.getDevice()} | grep -w inet"
        ]

        // Find the IP address for the Ethernet interface
        String ipAddrShowOutput = utilsService.executeLinuxCommand(command)
        def ipAddress = StringUtils.substringBetween(ipAddrShowOutput, "inet ", "/")
        Integer cloudPort = (Integer) (grailsApplication.config.cloudProxy.cloudPort)
        command = [
                "/bin/sh",
                "-c",
                "ss -n | grep ${ipAddress} | grep ${cloudPort}"
        ]

        String cloudEtherConn = utilsService.executeLinuxCommand(command)
        return cloudEtherConn == "" ? EthernetStatusEnum.notConnectedViaEthernet : EthernetStatusEnum.connectedViaEthernet
    }

}
