package com.securitycam.services

import com.google.common.collect.ImmutableMap
import com.securitycam.commands.SetUpWifiCommand
import com.securitycam.commands.SetWifiStatusCommand
import com.securitycam.configuration.Config
import com.securitycam.enums.EthernetStatusEnum
import com.securitycam.interfaceobjects.ConnectionDetails
import com.securitycam.interfaceobjects.CurrentWifiConnection
import com.securitycam.interfaceobjects.IpAddressDetails
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.interfaceobjects.RestfulResponse
import com.securitycam.enums.PassFail
import com.securitycam.interfaceobjects.WifiConnectResult
import groovy.json.JsonSlurper
import org.apache.activemq.util.URISupport
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

//import org.apache.commons.lang.StringUtils
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class WifiUtilsService {
    @Autowired
    LogService logService
    @Autowired
    UtilsService utilsService

    @Autowired
    RestfulInterfaceService restfulInterfaceService

    @Autowired
    Config config

    static final ImmutableMap<EthernetStatusEnum, String> ethernetConnectionStatus =
            ImmutableMap.of(
                    EthernetStatusEnum.connectedViaEthernet, "CONNECTED_VIA_ETHERNET",
                    EthernetStatusEnum.notConnectedViaEthernet, "NOT_CONNECTED_VIA_ETHERNET",
                    EthernetStatusEnum.noEthernet, "NO_ETHERNET")

    def scanWifi() {
        ObjectCommandResponse result = new ObjectCommandResponse()
        getActiveIPAddresses()

        try {
            final int scanWifiTimeout = 60
            RestfulResponse resp =
                    restfulInterfaceService.sendRequest("localhost:8000", "/",
                            "{\"command\": \"scanwifi\"}",
                            true, scanWifiTimeout)

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
            EthernetStatusEnum connStatus = isConnectedThroughEthernet(cmd.isCloud)
            final int setUpWifiTimeout = 180
            if (connStatus == EthernetStatusEnum.connectedViaEthernet) {
                RestfulResponse resp = cmd.password != null
                        ?
                        restfulInterfaceService.sendRequest("localhost:8000", "/",
                                "{\"command\": \"setupwifi\", \"ssid\": \"${cmd.ssid}\", \"password\": \"${cmd.password}\"}",
                                true, setUpWifiTimeout)
                        :
                        restfulInterfaceService.sendRequest("localhost:8000", "/",
                                "{\"command\": \"setupwifi\", \"ssid\": \"${cmd.ssid}\"}",
                                true, setUpWifiTimeout)

                if (resp.responseCode == 200)
                    result.responseObject = resp.responseObject
                else {
                    result.errno = resp.responseCode
                    result.responseObject = new WifiConnectResult(resp.responseCode, resp.errorMsg)
                    result.error = resp.getErrorMsg()
                    result.status = PassFail.FAIL
                }
            } else {
                final String warningMessage = "setUpWifi: Cannot change Wi-Fi settings when not connected through Ethernet."
                result.errno = 400
                logService.cam.warn(warningMessage)
                result.responseObject = new WifiConnectResult(-1, warningMessage)  // -1 as return code
                result.status = PassFail.FAIL
                result.error = warningMessage
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
            final int checkwifistatusTimeout = 60
            RestfulResponse resp =
                    restfulInterfaceService.sendRequest("localhost:8000", "/",
                            "{\"command\": \"checkwifistatus\"}",
                            true, checkwifistatusTimeout)
            if (resp.responseCode == 200) {
                JsonSlurper parser = new JsonSlurper()
                def json = parser.parseText(resp.responseObject['response'] as String)
                result.responseObject = json
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
            EthernetStatusEnum status = isConnectedThroughEthernet(cmd.isCloud)

            if (cmd.status != "off" || status == EthernetStatusEnum.connectedViaEthernet) {
                final int setwifistatusTimeout = 60
                RestfulResponse resp =
                        restfulInterfaceService.sendRequest("localhost:8000", "/",
                                "{\"command\": \"setwifistatus\", \"status\": \"" + cmd.status + "\"}",
                                true, setwifistatusTimeout)
                if (resp.responseCode == 200) {
                    JsonSlurper parser = new JsonSlurper()
                    def json = parser.parseText(resp.responseObject['response'] as String)
                    result.responseObject = json
                } else {
                    result.errno = resp.responseCode
                    result.status = PassFail.FAIL
                    result.error = resp.getErrorMsg()
                }
            } else {
                result.errno = 400
                result.status = PassFail.FAIL
                result.error = "Must be connected via Ethernet to set Wifi status to off"
            }
        }
        catch (Exception ex) {
            logService.cam.error("Exception in setWifiStatus: " + ex.getCause() + ' ' + ex.getMessage())
            result.errno = 500
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
            final int getactiveconnectionsTimeout = 60
            RestfulResponse resp =
                    restfulInterfaceService.sendRequest("localhost:8000", "/",
                            "{\"command\": \"getactiveconnections\"}",
                            true, getactiveconnectionsTimeout)
            if (resp.responseCode == 200) {
                JsonSlurper parser = new JsonSlurper()
                def json = parser.parseText(resp.responseObject['response'] as String)

                for (obj in json) {
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

    ObjectCommandResponse checkConnectedThroughEthernet(boolean isCloud = true) {
        ObjectCommandResponse result = new ObjectCommandResponse()
        EthernetStatusEnum es = isConnectedThroughEthernet(isCloud)
        if (es != EthernetStatusEnum.error)
            result.responseObject = [status: ethernetConnectionStatus[es]]
        else {
            result.status = PassFail.FAIL
            result.error = "Failed to get ethernet connection status"
        }
        return result
    }

    private EthernetStatusEnum isConnectedThroughEthernet(boolean isCloud) {
        try {
            final int getactiveconnectionsTimeout = 60
            ArrayList<ConnectionDetails> cdList = new ArrayList<>()
            RestfulResponse resp =
                    restfulInterfaceService.sendRequest("localhost:8000", "/",
                            "{\"command\": \"getactiveconnections\"}",
                            true, getactiveconnectionsTimeout)
            if (resp.responseCode == 200) {
                JsonSlurper parser = new JsonSlurper()
                def json = parser.parseText(resp.responseObject['response'] as String)

                for (obj in json) {
                    ConnectionDetails cd = new ConnectionDetails(obj.GENERAL_CONNECTION as String,
                            obj.GENERAL_HWADDR as String,
                            obj.GENERAL_TYPE as String,
                            obj.GENERAL_DEVICE as String)
                    if (cd.con_type == "ethernet")
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

            final String activeMQURL = config.cloudProxy.cloudActiveMQUrl
            String strUri = activeMQURL.startsWith("failover://") ? URISupport.stripPrefix(activeMQURL, "failover://") : activeMQURL
            URI uri = new URI(strUri)

            final int activeMQPort = uri.port

            Integer port = (Integer) (isCloud ? activeMQPort:
                    config.nvrWebServer.port)
            command = [
                    "/bin/sh",
                    "-c",
                    "ss -n | grep ${ipAddress} | grep ${port}"
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
