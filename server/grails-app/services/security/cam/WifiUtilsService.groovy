package security.cam

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import org.apache.commons.lang.StringUtils
import security.cam.commands.SetUpWifiCommand
import security.cam.commands.SetWifiStatusCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ConnectionDetails
import security.cam.interfaceobjects.EthernetStatusEnum
import security.cam.interfaceobjects.IpAddressDetails
import security.cam.interfaceobjects.ObjectCommandResponse
import security.cam.interfaceobjects.RestfulResponse

import java.util.regex.Matcher
import java.util.regex.Pattern

@Transactional
class WifiUtilsService {
    LogService logService
    UtilsService utilsService
    GrailsApplication grailsApplication
    RestfulInterfaceService restfulInterfaceService

    def scanWifi() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        getActiveIPAddresses()

        try {
            RestfulResponse resp =
                    restfulInterfaceService.sendRequest("localhost:8000", "/",
                            "{\"command\": \"scanwifi\"}",
                            true, 60)

            if (resp.responseCode == 200) {
                JsonSlurper parser = new JsonSlurper()
                def json = parser.parseText(resp.responseObject.response as String)
                result.responseObject = json
            }
            else {
                result.status = PassFail.FAIL
                result.error = resp.getErrorMsg()
            }
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
                RestfulResponse resp = cmd.password != null
                        ?
                        restfulInterfaceService.sendRequest("localhost:8000", "/",
                                "{\"command\": \"setupwifi\", \"ssid\": \"${cmd.ssid}\", \"password\": \"${cmd.password}\"}",
                                true, 180)
                        :
                        restfulInterfaceService.sendRequest("localhost:8000", "/",
                                "{\"command\": \"setupwifi\", \"ssid\": \"${cmd.ssid}\"}",
                                true, 180)

                if (resp.responseCode == 200)
                    result.responseObject = resp.responseObject['response']
                else {
                    result.status = PassFail.FAIL
                    result.error = resp.getErrorMsg()
                }

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
            if (resp.responseCode == 200)
                result.responseObject = resp.responseObject['response']
            else {
                result.status = PassFail.FAIL
                result.error = resp.getErrorMsg()
            }
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
                if (resp.responseCode == 200)
                    result.responseObject = resp.responseObject['response']
                else {
                    result.status = PassFail.FAIL
                    result.error = resp.getErrorMsg()
                }
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

    /**
     * getActiveIPAddresses: Get the active IP addresses and their associated interface details
     * @return:
     */
    def getActiveIPAddresses()
    {
        ObjectCommandResponse result = new ObjectCommandResponse()
        ArrayList<IpAddressDetails> ipDets = new ArrayList<>()

        try {
            ArrayList<ConnectionDetails> cdList = getActiveConnections()

            StringBuilder sb = new StringBuilder()
            // Build up the regex used with grep to get the inet lines for the active interfaces
            boolean isFirst = true
            for(ConnectionDetails cd in cdList) {
                if(!isFirst)
                    sb.append("\\|")
                else
                    isFirst = false

                sb.append("${cd.getDevice()}")
            }
            String[] command = [
                    "sh",
                    "-c",
                    "ip addr show | grep -w inet | grep '${sb.toString()}'"
            ]
            String[] res = utilsService.executeLinuxCommand(command).split("\n")

            for(String line in res) {
                String ip = StringUtils.substringBetween(line, 'inet ', '/')
                String iface = line.substring(line.lastIndexOf(" ")+1, line.length())
                ConnectionDetails cd = cdList.find((cdets) -> {
                    return iface == cdets.device
                })

                IpAddressDetails ipad = new IpAddressDetails(ip, cd)
                ipDets.add(ipad)
            }
            result.responseObject = ipDets
        }
        catch(Exception ex)
        {
            logService.cam.error("Exception in getActiveIPAddresses: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    private ArrayList<ConnectionDetails> getActiveConnections()
    {
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
        return cdList
    }

    EthernetStatusEnum checkConnectedThroughEthernet() {
        try {
            ArrayList<ConnectionDetails> cdList = getActiveConnections()
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
        catch(Exception ex)
        {
            logService.cam.error("${ex.getClass().getName()} in checkConnectedThroughEthernet: ${ex.getCause()}-${ex.getMessage()}")
            return EthernetStatusEnum.error
        }
    }
}
