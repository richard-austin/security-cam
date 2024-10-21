package com.securitycam.controllers

import com.securitycam.commands.SetUpWifiCommand
import com.securitycam.interfaceobjects.Greeting
import com.securitycam.interfaceobjects.HelloMessage
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.services.LogService
import com.securitycam.validators.SetupWifiValidator
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.util.HtmlUtils

@Controller
class WifiUtilsController {
// Commented out in favour of index.html in resources/static
//    @GetMapping("/")
//    String index() {
//        return "Greetings from Spring Boot!";
//    }

    @Autowired
    SetupWifiValidator setupWifiValidator

    @Autowired
    SimpMessagingTemplate brokerMessagingTemplate
    @Autowired
    LogService logService

    // @Secured(['ROLE_CLOUD', 'ROLE_CLIENT'])
    @RequestMapping('setUpWifi')
    ResponseEntity<?> setUpWifi(@RequestBody final SetUpWifiCommand cmd) {
        ObjectCommandResponse result
        Errors errors = new BeanPropertyBindingResult(cmd, "setupwifi");
        setupWifiValidator.validate(cmd, errors);
        if (errors.hasErrors()) {
            errors.allErrors.forEach {
                System.out.println(it)
            }
//            def errorsMap = validationErrorService.commandErrors(cmd.errors as ValidationErrors, 'setUpWifi')
//            logService.cam.error "setUpWifi: Validation error: " + errorsMap.toString()
//            render(status: 400, text: errorsMap as JSON)
            return ResponseEntity.badRequest().body(errors.getAllErrors())
        } else {
//            result = wifiUtilsService.setUpWifi(cmd)
//
//            if (result.status == PassFail.PASS) {
//                render(status: 200, text: result.responseObject)
//            } else {
//                logService.cam.error "setUpWifi: error: ${result.error}"
//                result.status = PassFail.FAIL
//                render(status: result.errno, text: result.responseObject as JSON)
//            }
           return ResponseEntity.ok('It Worked!')
        }
    }

    @Secured(['ROLE_CLOUD', 'ROLE_CLIENT'])

    @GetMapping("/h2")
    def checkWifiStatus(Model model) {
        ModelAndView mav = new ModelAndView("hello")
        mav.addObject("message", "This is text from the controller function")
        return mav
//        ObjectCommandResponse result = wifiUtilsService.checkWifiStatus()
//        if (result.status == PassFail.PASS) {
//            render(status: 200, text: result.responseObject)
//        } else {
//            logService.cam.error "checkWifiStatus: error: ${result.error}"
//            result.status = PassFail.FAIL
//            render(status: 500, text: result.error)
//        }
    }

    @PostMapping("/setupWifi2")
    @Secured("ROLE_CLIENT")
    ResponseEntity<?> setupWifi2(@Valid @RequestBody SetUpWifiCommand cmd) {
        Errors errors = new BeanPropertyBindingResult(cmd, "setupwifi");
        setupWifiValidator.validate(cmd, errors);
        if (errors.hasErrors()) {
            errors.allErrors.forEach {
                System.out.println(it)
            }
        }

//        if(result.hasErrors()) {
//            System.out.println("There are errors")
//            return ResponseEntity.badRequest()
//        }
        else {
            logService.setLogLevel("DEBUG")
            logService.cam.debug("About to write to topic/greetings")
            brokerMessagingTemplate.convertAndSend("/topic/greetings", new Greeting("This is the payload"))
            return ResponseEntity.ok("Wifi settings are valid")
        }


//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
//        Map<String, String> errors = new HashMap<>();
//        ex.getBindingResult().getAllErrors().forEach((error) -> {
//            String fieldName = ((FieldError) error).getField();
//            String errorMessage = error.defaultMessage;
//            errors.put(fieldName, errorMessage);
//        });
//        return errors;
    }

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    Greeting greeting(HelloMessage message) throws Exception {
        //Thread.sleep(1000); // simulated delay
        return new Greeting("Hello, " + HtmlUtils.htmlEscape(message.getName()) + "!");
    }

}
