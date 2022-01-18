import ch.qos.logback.core.util.FileSize
import grails.util.BuildSettings
import grails.util.Environment
import org.springframework.boot.logging.logback.ColorConverter
import org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter

import java.nio.charset.StandardCharsets

conversionRule 'clr', ColorConverter
conversionRule 'wex', WhitespaceThrowableProxyConverter

// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        charset = StandardCharsets.UTF_8

        pattern =
                '%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} ' + // Date
                        '%clr(%5p) ' + // Log level
                        '%clr(---){faint} %clr([%15.15t]){faint} ' + // Thread
                        '%clr(%-40.40logger{39}){cyan} %clr(:){faint} ' + // Logger
                        '%m%n%wex' // Message
    }
}

def targetDir = BuildSettings.TARGET_DIR
if (Environment.isDevelopmentMode() && targetDir != null) {
    appender("FULL_STACKTRACE", FileAppender) {
        file = "${targetDir}/stacktrace.log"
        append = true
        encoder(PatternLayoutEncoder) {
            charset = StandardCharsets.UTF_8
            pattern = "%level %logger - %msg%n"
        }
    }
    logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false)
}

appender("camLogAppender", RollingFileAppender) {

    def path = "/home/security-cam/logs/"
    file = "${path}security-cam.log"
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36} [%thread] - %M:%L - %msg%n"
    }

    rollingPolicy(SizeAndTimeBasedRollingPolicy) {
        FileNamePattern = "${path}security-cam.log-%d{yyyy-MM-dd}.%i"
        maxHistory = 10
        totalSizeCap = new FileSize(3000000000)
        MaxFileSize = new FileSize(10000000)
    }
}

appender("cloudProxyLogAppender", RollingFileAppender) {

    def path = "/home/security-cam/logs/"
    file = "${path}cloud-proxy.log"
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36} [%thread] - %M:%L - %msg%n"
    }

    rollingPolicy(SizeAndTimeBasedRollingPolicy) {
        FileNamePattern = "${path}cloud-proxy.log-%d{yyyy-MM-dd}.%i"
        maxHistory = 10
        totalSizeCap = new FileSize(3000000000)
        MaxFileSize = new FileSize(10000000)
    }
}


logger('CAM', DEBUG, ['camLogAppender'], true)
logger('CLOUDPROXY', DEBUG, ['cloudProxyLogAppender'], true)

root(ERROR, ['STDOUT'])
