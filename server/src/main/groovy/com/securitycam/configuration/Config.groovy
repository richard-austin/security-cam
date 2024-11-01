package com.securitycam.configuration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration


@Configuration
@ConfigurationProperties(prefix = "app.motion")
class Motion {
    String configDir
    String maskFileDir
    String recordingDir
}

@Configuration
@ConfigurationProperties(prefix = "nvrwebserver")
class NVRWebServer {
    int port
}

@Configuration
@ConfigurationProperties(prefix = "cloudproxy")
class CloudProxy {
    boolean enabled
    String productKeyPath
    String cloudActiveMQUrl
    String activeMQInitQueue
    String webServerForCloudProxyHost
    int webServerForCloudProxyPort
    String logLevel
}

@Configuration
@ConfigurationProperties(prefix = "app")
class Config {
    String camerasHomeDirectory
    String appHomeDirectory
    String recordingsHomeDirectory
    String myipFileLocation
    String logLevel
    int camAdminHostPort

    @Autowired
    Motion motion

    @Autowired
    NVRWebServer nvrWebServer

    @Autowired
    CloudProxy cloudProxy
}
