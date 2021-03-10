package security.cam

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import grails.gorm.transactions.Transactional
import org.slf4j.LoggerFactory

import javax.annotation.PostConstruct

@Transactional
class LogService {

    Logger cam = null
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
        setLogLevel('DEBUG')
    }
}
