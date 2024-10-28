package com.securitycam.services

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.proxy.ILogService
import com.securitycam.configuration.Config
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jakarta.annotation.PostConstruct

@Transactional
@Service
class LogService implements ILogService {

    Logger cam = null
    @Autowired
    Config config

    static Logger logger = null

    void setLogLevel(String level)
    {
        cam.setLevel(level=='INFO' ? Level.INFO :
                     level=='DEBUG' ? Level.DEBUG :
                     level=='TRACE' ? Level.TRACE :
                     level=='WARN' ? Level.WARN :
                     level=='ERROR' ? Level.ERROR :
                     level=='OFF' ? Level.OFF :
                     level=='ALL' ? Level.ALL : Level.OFF)
    }

    LogService() {
        cam = (Logger) LoggerFactory.getLogger('CAM')
        LogService.logger = cam
    }

    @PostConstruct
    def initialise() {
        setLogLevel(config.logLevel as String)
    }
}
