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
@ConfigurationProperties(prefix = "app")
class Config {
    String camerasHomeDirectory
    String appHomeDirectory
    String recordingsHomeDirectory
    String myipFileLocation
    String logLevel

    @Autowired
    Motion motion
}
