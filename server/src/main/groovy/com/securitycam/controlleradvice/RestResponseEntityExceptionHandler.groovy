package com.securitycam.controlleradvice

import com.securitycam.error.NVRRestMethodException
import com.securitycam.services.LogService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.ui.Model
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import org.springframework.web.servlet.resource.NoResourceFoundException
import org.springframework.web.servlet.view.RedirectView

//@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
class RestResponseEntityExceptionHandler {
    @Autowired
    LogService logService

    // Exception handler for invalid method arguments
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>()
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField()
            String errorMessage = error.getDefaultMessage()
            errors.put(fieldName, errorMessage)
        })
        logService.cam.warn("MethodArgumentNotValidException ${errors.toString()}")
        return ResponseEntity.badRequest().body(errors)
    }

   @ExceptionHandler(NoResourceFoundException.class)
    RedirectView handleNoResourceFoundException(RedirectAttributes redirectAttributes, NoResourceFoundException ex) {
       def resourcePath = ex.getResourcePath()
       def message = ex.getMessage()

       redirectAttributes.addFlashAttribute("resourcePath", resourcePath)
       redirectAttributes.addFlashAttribute("message", message)
       return new RedirectView("/notFound") //  new ResponseEntity<Object>(retVal, HttpStatus.NOT_FOUND).body("notFound")
    }

    @ExceptionHandler(NVRRestMethodException.class)
    ResponseEntity<Object> handleNVRRestMethodException(NVRRestMethodException ex) {
        logService.cam.error("${ex.getClass()}: ${ex.getReason()}")
        logService.cam.error(ex.getStackTrace().toString())
        ErrorResponse retVal = new ErrorResponse(ex)
        return ResponseEntity.internalServerError().body(retVal)
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleGeneralException(Exception ex) {
        logService.cam.error("${ex.getClass()} has occurredc: ${ex.getMessage()}: ${ex.getCause()}")
 //       logService.cam.error(ex.getStackTrace().toString())
        ErrorResponse retVal = new ErrorResponse(ex)
        return ResponseEntity.internalServerError().body(retVal)
    }
}

class ErrorResponse {
    Object exception
    String error
    String reason
    ErrorResponse(Object exception, String error, String reason) {
        this.exception = exception
        this.error = error
        this.reason = reason
    }

    ErrorResponse(Exception ex) {
        exception = ex.class
        error = ex.getMessage()
        reason = "Caused by: " + ex.getCause()
    }

    ErrorResponse(NVRRestMethodException ex) {
        exception = ex.class
        error = ex.getMessage()
        reason = ex.getReason()
    }
}
