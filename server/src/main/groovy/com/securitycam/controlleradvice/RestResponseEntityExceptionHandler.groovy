package com.securitycam.controlleradvice

import com.securitycam.error.NVRRestMethodException
import com.securitycam.services.LogService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

//@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
class RestResponseEntityExceptionHandler {
    @Autowired
    LogService logService

    // Exception handler for invalid method arguments
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        logService.cam.warn("MethodArgumentNotValidException ${errors.toString()}")
        return new ResponseEntity<Object>(errors, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(NVRRestMethodException.class)
    ResponseEntity<Object> handleNVRRestMethodException(NVRRestMethodException ex) {
        logService.cam.error("${ex.getClass()} in ${ex.getRequestUri()}: ${ex.getMessage()}: ${ex.getReason()}")
        logService.cam.error(ex.getStackTrace().toString())
        ErrorResponse retVal = new ErrorResponse(ex)
        return new ResponseEntity<Object>(retVal, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleGeneralException(Exception ex) {
        logService.cam.error("${ex.getClass()} has occurredc: ${ex.getMessage()}: ${ex.getCause()}")
        logService.cam.error(ex.getStackTrace().toString())
        ErrorResponse retVal = new ErrorResponse(ex)
        return new ResponseEntity<Object>(retVal, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}

class ErrorResponse {
    Object exception
    String request
    String error
    String reason
    ErrorResponse(Object exception, String request, String error, String reason) {
        this.exception = exception
        this.request = request
        this.error = error
        this.reason = reason
    }

    ErrorResponse(Exception ex) {
        exception = ex.class
        request = "?"
        error = ex.getMessage()
        reason = ex.getCause()
    }

    ErrorResponse(NVRRestMethodException ex) {
        exception = ex.class
        request = ex.getRequestUri()
        error = ex.getMessage()
        reason = ex.getReason()
    }
}
