package com.securitycam.controlleradvice

import com.securitycam.error.NVRRestMethodException
import com.securitycam.services.LogService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
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
    ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException ex, final HttpServletRequest req) {
        Map<String, String> errors = new HashMap<>()
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField()
            String errorMessage = error.getDefaultMessage()
            errors.put(fieldName, errorMessage)
        })
        logService.cam.warn("MethodArgumentNotValidException ${errors.toString()}")
        String acceptHeader = req.getHeader("Accept")
        if (acceptHeader.containsIgnoreCase("application/json") || acceptHeader.contains('*/*') )
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(errors)
        else // Don't return json if the the request cannot handle it
            return ResponseEntity.internalServerError().contentType(new MediaType(acceptHeader)).body('')
    }

   @ExceptionHandler(NoResourceFoundException.class)
    RedirectView handleNoResourceFoundException(RedirectAttributes redirectAttributes, NoResourceFoundException ex) {
       def requestUri = ex.getResourcePath()
       def message = ex.getMessage()
       def status = ex.statusCode

       redirectAttributes.addFlashAttribute("requestUri", requestUri)
       redirectAttributes.addFlashAttribute("message", message)
       redirectAttributes.addFlashAttribute("status", status)
       return new RedirectView("/error") //  new ResponseEntity<Object>(retVal, HttpStatus.NOT_FOUND).body("notFound")
    }

    @ExceptionHandler(NVRRestMethodException.class)
    ResponseEntity<Object> handleNVRRestMethodException(final NVRRestMethodException ex, final HttpServletRequest req) {
        logService.cam.error("${ex.getClass()}: ${ex.getReason()}")
        logService.cam.trace(ex.getStackTrace().toString())
        return returnDetails(ex, req)
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleGeneralException(final Exception ex, final HttpServletRequest req) {
        logService.cam.error("${ex.getClass()} has occurred: ${ex.getMessage()}: ${ex.getCause()}")
        return returnDetails(ex, req)
    }

    static ResponseEntity<Object> returnDetails(Exception ex, HttpServletRequest req) {
        String acceptHeader = req.getHeader("Accept")
        if (acceptHeader.containsIgnoreCase("application/json") || acceptHeader.contains('*/*')) {
            ErrorResponse retVal = new ErrorResponse(ex)
            return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(retVal)
        } else // Don't return json if the the request cannot handle it
            return ResponseEntity.internalServerError().contentType(new MediaType(acceptHeader)).body('')
    }
}

class ErrorResponse {
    String exception
    String error
    String reason
    ErrorResponse(Exception ex, String error, String reason) {
        this.exception = ex.class.getName()
        this.error = error
        this.reason = reason
    }

    ErrorResponse(Exception ex) {
        exception = ex.class.getName()
        error = ex.getMessage()
        reason = ex.getCause() != null ? ("Caused by: " + ex.getCause().getClass().getName() + ": " +  ex.getCause().getMessage()) : ""
    }

    ErrorResponse(NVRRestMethodException ex) {
        exception = ex.getClass().getName()
        error = ex.getMessage()
        reason = ex.getReason()
    }
}
