package com.securitycam.services

import com.securitycam.controlleradvice.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class RestfulCallErrorService {
    ResponseEntity<ErrorResponse> returnError(Throwable ex, String request, String error, String reason, HttpStatus status) {
        return new ResponseEntity<ErrorResponse>(new ErrorResponse(ex.getClass(), request, error, reason), status)
    }
}
