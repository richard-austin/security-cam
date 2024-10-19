package com.securitycam.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app")
class Config {
    String camerasHomeDirectory
    String appHomeDirectory
    String recordingsHomeDirectory
    String myipFileLocation
    String logLevel
}
