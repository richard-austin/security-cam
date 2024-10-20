package com.securitycam.controlleradvice

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = [ResponseStatusException.class, Exception.class])
    defaultErrorHandler(HttpServletRequest req, Exception ex) throws Exception {
        if(ex instanceof ResponseStatusException)
            return new ResponseEntity<Object>([exception: ex.getClass(), request: req.requestURI, error: ex.getMessage(), reason: ""], ex.getStatusCode())
        else
            return new ResponseEntity<Object>([exception: ex.getCause().getClass(), request: req.requestURI, error: ex.getMessage(), reason: ""], HttpStatus.INTERNAL_SERVER_ERROR)
    }
        // If the exception is annotated with @ResponseStatus rethrow it and let

//    protected ResponseEntity<Object> handleConflict(Exception ex, WebRequest request) {
//        if(ex instanceof ResponseStatusException)
//            return new ResponseEntity<Object>([exception: ex.getClass(), request: request.getDescription(false), error: ex.getMessage(), reason: ex.getReason()], ex.getStatusCode())
//        else
//            return new ResponseEntity<Object>([exception: ex.getClass(), request: request.getDescription(false), error: ex.getMessage(), reason: ""], HttpStatus.INTERNAL_SERVER_ERROR)
//    }
}
