package security.cam

import com.google.common.collect.ImmutableMap
import grails.converters.JSON
import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import org.apache.commons.lang.StringUtils
import security.cam.commands.SetUpWifiCommand
import security.cam.commands.SetWifiStatusCommand
import security.cam.enums.PassFail
import security.cam.interfaceobjects.ConnectionDetails
import security.cam.interfaceobjects.CurrentWifiConnection
import security.cam.interfaceobjects.EthernetStatusEnum
import security.cam.interfaceobjects.IpAddressDetails
import security.cam.interfaceobjects.ObjectCommandResponse
import security.cam.interfaceobjects.RestfulResponse
import security.cam.interfaceobjects.WifiConnectResult

@Transactional
class WifiUtilsService {
    LogService logService
    UtilsService utilsService
    GrailsApplication grailsApplication
    RestfulInterfaceService restfulInterfaceService

    static final ImmutableMap<EthernetStatusEnum, String> ethernetConnectionStatus =
            ImmutableMap.of(
                    EthernetStatusEnum.connectedViaEthernet, "CONNECTED_VIA_ETHERNET",
                    EthernetStatusEnum.notConnectedViaEthernet, "NOT_CONNECTED_VIA_ETHERNET",
                    EthernetStatusEnum.noEthernet, "NO_ETHERNET")

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
            } else {
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
            EthernetStatusEnum status = isConnectedThroughEthernet()

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
                    result.responseObject = resp.responseObject as JSON
                else {
                    result.errno = resp.responseCode
                    result.responseObject = new WifiConnectResult(resp.getErrorMsg())
                    result.error = resp.getErrorMsg()
                    result.status = PassFail.FAIL
                }

            } else {
                result.errno = 400
                result.status = PassFail.FAIL
                result.responseObject = new WifiConnectResult("Must be connected via Ethernet to set up a Wifi Connection")
                result.error = result.responseObject.message
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
            if (resp.responseCode == 200) {
                JsonSlurper parser = new JsonSlurper()
                def json = parser.parseText(resp.responseObject['response'] as String)
                result.responseObject = json as JSON
            } else {
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
            EthernetStatusEnum status = isConnectedThroughEthernet()

            if (cmd.status != "off" || status == EthernetStatusEnum.connectedViaEthernet) {
                RestfulResponse resp =
                        restfulInterfaceService.sendRequest("localhost:8000", "/",
                                "{\"command\": \"setwifistatus\", \"status\": \"" + cmd.status + "\"}",
                                true, 60)
                if (resp.responseCode == 200) {
                    JsonSlurper parser = new JsonSlurper()
                    def json = parser.parseText(resp.responseObject['response'] as String)
                    result.responseObject = json as JSON
                } else {
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

    def getCurrentWifiConnection() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        try {
            String[] lines = utilsService.executeLinuxCommand('iwconfig').split('\n')
            String accessPoint = ""
            for (line in lines)
                if (line.contains("SSID")) {
                    accessPoint = StringUtils.substringBetween(line, 'SSID:"', '"')
                    break
                }

            CurrentWifiConnection cwc = new CurrentWifiConnection(accessPoint, lines)
            result.responseObject = cwc
        }
        catch (Exception ex) {
            logService.cam.error("${ex.getClass().getName()} in getCurrentWifiConnection: ${ex.getCause()} ${ex.getMessage()}")
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }

        return result
    }

    /**
     * getActiveIPAddresses: Get the active IP addresses and their associated interface details
     * @return:
     */
    def getActiveIPAddresses() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        ArrayList<IpAddressDetails> ipDets = new ArrayList<>()

        try {
            RestfulResponse resp =
                    restfulInterfaceService.sendRequest("localhost:8000", "/",
                            "{\"command\": \"getactiveconnections\"}",
                            true, 60)
            if (resp.responseCode == 200) {
                JsonSlurper parser = new JsonSlurper()
                def json = parser.parseText(resp.responseObject['response'] as String)

                for(obj in json)
                {
                    // We have to fill in the name for ethernet as it's not managed by nmcli, which gives no name for it
                    ConnectionDetails cd = new ConnectionDetails(obj.GENERAL_TYPE == "ethernet" ? "Wired connection" : obj.GENERAL_CONNECTION as String,
                            obj.GENERAL_HWADDR as String,
                            obj.GENERAL_TYPE as String,
                            obj.GENERAL_DEVICE as String)
                    // We're only dealing with one IP address
                    IpAddressDetails ipad = new IpAddressDetails(obj.IP4_ADDRESS_1 as String, cd)
                    ipDets.add(ipad)
                }
                result.responseObject = ipDets
            } else {
                result.status = PassFail.FAIL
                result.error = resp.getErrorMsg()
            }
            result.responseObject = ipDets
        }
        catch (Exception ex) {
            logService.cam.error("Exception in getActiveIPAddresses: " + ex.getCause() + ' ' + ex.getMessage())
            result.status = PassFail.FAIL
            result.error = ex.getMessage()
        }
        return result
    }

    ObjectCommandResponse checkConnectedThroughEthernet() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        EthernetStatusEnum es = isConnectedThroughEthernet()
        if (es != EthernetStatusEnum.error)
            result.responseObject = [status: ethernetConnectionStatus[es]]
        else {
            result.status = PassFail.FAIL
            result.error = "Failed to get ethernet connection status"
        }
        return result
    }

    private EthernetStatusEnum isConnectedThroughEthernet() {
        try {
            ArrayList<ConnectionDetails> cdList = new ArrayList<>()

            RestfulResponse resp =
                    restfulInterfaceService.sendRequest("localhost:8000", "/",
                            "{\"command\": \"getactiveconnections\"}",
                            true, 60)
            if (resp.responseCode == 200) {
                JsonSlurper parser = new JsonSlurper()
                def json = parser.parseText(resp.responseObject['response'] as String)

                for (obj in json) {
                    ConnectionDetails cd = new ConnectionDetails(obj.GENERAL_CONNECTION as String,
                            obj.GENERAL_HWADDR as String,
                            obj.GENERAL_TYPE as String,
                            obj.GENERAL_DEVICE as String)
                    if(cd.con_type == "ethernet")
                        cdList.add(cd)
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
        catch (Exception ex) {
            logService.cam.error("${ex.getClass().getName()} in checkConnectedThroughEthernet: ${ex.getCause()}-${ex.getMessage()}")
            return EthernetStatusEnum.error
        }
    }
}
