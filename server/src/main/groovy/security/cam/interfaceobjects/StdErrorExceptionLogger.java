package security.cam.interfaceobjects;

import org.apache.http.ExceptionLogger;
import org.jetbrains.annotations.NotNull;
import security.cam.LogService;

public class StdErrorExceptionLogger implements ExceptionLogger {
    LogService logService;
    public StdErrorExceptionLogger(LogService logService) {
        this.logService = logService;
    }
    @Override
    public void log(@NotNull Exception ex) {
        logService.getCam().error(ex.getClass().getName()+": "+ex.getMessage());
    }
}
