package com.securitycam.controllers

import com.securitycam.commands.AddOrUpdateActiveMQCredsCmd
import com.securitycam.commands.ChangeEmailCommand
import com.securitycam.commands.CheckNotGuestCommand
import com.securitycam.commands.CreateOrUpdateAccountCommand
import com.securitycam.commands.ResetPasswordCommand
import com.securitycam.commands.SetupGuestAccountCommand
import com.securitycam.dao.RoleRepository
import com.securitycam.dao.UserRepository
import com.securitycam.enums.PassFail
import com.securitycam.error.NVRRestMethodException
import com.securitycam.interfaceobjects.ObjectCommandResponse
import com.securitycam.security.TwoFactorAuthenticationProvider
import com.securitycam.services.LogService
import com.securitycam.services.UserAdminService
import com.securitycam.services.UtilsService
import com.securitycam.validators.AddOrUpdateActiveMQCredsCmdValidator
import com.securitycam.validators.BadRequestResult
import com.securitycam.validators.ChangeEmailCommandValidator
import com.securitycam.validators.CheckNotGuestCommandValidator
import com.securitycam.validators.CreateOrUpdateAccountCommandValidator
import com.securitycam.validators.GeneralValidator
import com.securitycam.validators.ResetPasswordCommandValidator
import com.securitycam.validators.SetupGuestAccountCommandValidator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user")
class UserController {

    @Autowired
    UserAdminService userAdminService

    @Autowired
    UserRepository userRepository
    @Autowired
    RoleRepository roleRepository
    @Autowired
    UtilsService utilsService

    @Autowired
    LogService logService

    @Autowired()
    TwoFactorAuthenticationProvider authenticationManager

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/changePassword")
    def changePassword(@RequestBody ResetPasswordCommand cmd) {
        ObjectCommandResponse resp
        def gv = new GeneralValidator(cmd, new ResetPasswordCommandValidator(authenticationManager))
        def result = gv.validate()

        if (result.hasErrors()) {
            def retVal = new BadRequestResult(result)
            logService.cam.error "changePassword: Validation error: "
            return new ResponseEntity<BadRequestResult>(retVal, HttpStatus.BAD_REQUEST)
        } else {
            resp = userAdminService.resetPassword(cmd)
            if (resp.status != PassFail.PASS) {
                throw new NVRRestMethodException(resp.error, "user/changePassword")
            } else {
                logService.cam.info("changePassword: success")
                return ""
            }
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @PostMapping("/getEmail")
    def getEmail() {
        ObjectCommandResponse result = userAdminService.getEmail()
        if (result.status != PassFail.PASS) {
            throw new NVRRestMethodException(result.error, "user/getEmail")
        } else {
            logService.cam.info("getEmail: success")
            return result.responseObject
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD'])
    @PostMapping("/changeEmail")
    def changeEmail(@RequestBody ChangeEmailCommand cmd) {
        ObjectCommandResponse resp

        def gv = new GeneralValidator(cmd, new ChangeEmailCommandValidator(authenticationManager, userRepository))
        def result = gv.validate()

        if (result.hasErrors()) {
            def retVal = new BadRequestResult(result)
            logService.cam.error "changeEmail: Validation error: "
            return new ResponseEntity<BadRequestResult>(retVal, HttpStatus.BAD_REQUEST)
        } else {
            resp = userAdminService.changeEmail(cmd)
            if (resp.status != PassFail.PASS) {
                throw new NVRRestMethodException(resp.error, "user/changeEmail")
            } else {
                logService.cam.info("changeEmail: success")
                return ""
            }
        }
    }

    @Secured(['ROLE_CLIENT'])
    @PostMapping("/setupGuestAccount")
    def setupGuestAccount(@RequestBody SetupGuestAccountCommand cmd) {
        ObjectCommandResponse resp
        def gv = new GeneralValidator(cmd, new SetupGuestAccountCommandValidator(userRepository))
        def result = gv.validate()
        if (result.hasErrors()) {
            def retVal = new BadRequestResult(result)
            logService.cam.error "setupGuestAccount: Validation error: "
            return new ResponseEntity<BadRequestResult>(retVal, HttpStatus.BAD_REQUEST)
        } else {
            resp = userAdminService.setupGuestAccount(cmd)

            if (resp.status != PassFail.PASS)
                throw new NVRRestMethodException(resp.error, "user/setupGuestAccount")
            else {
                logService.cam.info("setupGuestAccount: (= ${resp.responseObject}) success")
                return resp.responseObject
            }
        }
    }

    @Secured(['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST'])
    @RequestMapping("isGuest")
    def isGuest() {
        ObjectCommandResponse result = userAdminService.isGuest()
        if (result.status != PassFail.PASS)
            throw new NVRRestMethodException(result.error, "user/isGuest")
        else
           return ResponseEntity.ok(result.responseObject)
    }

    @Secured(['ROLE_CLIENT'])
    @RequestMapping("guestAccountEnabled")
    def guestAccountEnabled() {
        ObjectCommandResponse result = userAdminService.guestAccountEnabled()
        if (result.status != PassFail.PASS)
            throw new NVRRestMethodException(result.error, "user/guestAccountEnabled")
        else
            return  ResponseEntity.ok(result.responseObject)
    }

    @Secured(['ROLE_CLOUD'])
    def createOrUpdateAccount(CreateOrUpdateAccountCommand cmd) {
        ObjectCommandResponse result
        def gv = new GeneralValidator(cmd, new CreateOrUpdateAccountCommandValidator(utilsService, userAdminService, userRepository, roleRepository))
        def results = gv.validate()
        if (results.hasErrors()) {
            def retVal = new BadRequestResult(results)
            logService.cam.error "createOrUpdateAccount: Validation error: ${retVal.toString()}"
            return new ResponseEntity<BadRequestResult>(retVal, HttpStatus.BAD_REQUEST)
        } else {
            result = userAdminService.createOrUpdateAccount(cmd)
            if (result.status != PassFail.PASS) {
                throw new NVRRestMethodException(result.error, "user/createOrUpdateAccount")
            } else {
                logService.cam.info("createOrUpdateAccount: success")
                return ResponseEntity.ok("")
            }
        }
    }

    /**
     * createOrUpdateAccountLocally: Unsecured to enable account creation without being logged in.
     *                       nginx requires a session to allow access to this url to prevent
     *                       unauthenticated external access. It is accessed locally on tomcats port 8080.
     *
     * @param cmd: Contains username, password, email, updateExisting
     */
    @PostMapping("createOrUpdateAccountLocally")
    def createOrUpdateAccountLocally(@RequestBody CreateOrUpdateAccountCommand cmd){
        def gv = new GeneralValidator(cmd, new CreateOrUpdateAccountCommandValidator(utilsService, userAdminService, userRepository, roleRepository))
        def result = gv.validate()
        if(result.hasErrors()) {
            def retVal = new BadRequestResult(result)
            logService.cam.error "createOrUpdateAccountLocally: Validation error: "
            return new ResponseEntity<BadRequestResult>(retVal, HttpStatus.BAD_REQUEST)
        }
        else
            createOrUpdateAccount(cmd)
    }

    /**
     * checkForAccountLocally: Unsecured to enable account creation without being logged in.
     *                       nginx requires a session to allow access to this url to prevent
     *                       unauthenticated external access. It is accessed locally through tomcats port 8080.
     */
    @PostMapping("/checkForAccountLocally")
    def checkForAccountLocally(CheckNotGuestCommand cmd) {
        def gv = new GeneralValidator(cmd, new CheckNotGuestCommandValidator(userAdminService))
        def result = gv.validate()
        if(result.hasErrors()) {
            def retVal = new BadRequestResult(result)
            logService.cam.error "checkForAccountLocally: Validation error: "
            return new ResponseEntity<BadRequestResult>(retVal, HttpStatus.BAD_REQUEST)
        }
        else
            hasLocalAccount()
    }

    @PostMapping("/checkForActiveMQCreds")
    def checkForActiveMQCreds(CheckNotGuestCommand cmd) {
        def gv = new GeneralValidator(cmd, new CheckNotGuestCommandValidator(userAdminService))
        def result = gv.validate()
        if(result.hasErrors()) {
            def retVal = new BadRequestResult(result)
            logService.cam.error "checkForActiveMQCreds: Validation error: "
            return new ResponseEntity<BadRequestResult>(retVal, HttpStatus.BAD_REQUEST)
        }
            hasActiveMQCreds()
    }

    @PostMapping("/addOrUpdateActiveMQCreds")
    def addOrUpdateActiveMQCreds(@RequestBody AddOrUpdateActiveMQCredsCmd cmd) {
        ObjectCommandResponse response

        def gv = new GeneralValidator(cmd, new AddOrUpdateActiveMQCredsCmdValidator(userAdminService))
        def result = gv.validate()
        if (result.hasErrors()) {
            def errorsMap = new BadRequestResult(result)
            logService.cam.error "addOrUpdateActiveMQCreds: Validation error: " + errorsMap.toString()
            return new ResponseEntity<BadRequestResult>(errorsMap, HttpStatus.BAD_REQUEST)
        } else {
            response = userAdminService.addOrUpdateActiveMQCreds(cmd)
            if (response.status != PassFail.PASS) {
                throw new NVRRestMethodException(response.error, "user/createOrUpdateAccount")
            } else {
                logService.cam.info("addOrUpdateActiveMQCreds: success")
                ResponseEntity.ok("")
            }
        }
    }

    @Secured(['ROLE_CLOUD'])
    @PostMapping("/hasLocalAccount")
    def hasLocalAccount() {
        ObjectCommandResponse result = userAdminService.hasLocalAccount()

        if (result.status != PassFail.PASS) {
            throw new NVRRestMethodException(result.error, "user/hasLocalAccount")
        } else {
            logService.cam.info("hasLocalAccount: (= ${result.responseObject}) success")
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result.responseObject != null)
        }
    }


    /**
     * removeAccount: Remove the local web account used for direct access to to NVR
     */
    @Secured(['ROLE_CLOUD'])
    @PostMapping("/removeAccount")
    def removeAccount()
    {
        ObjectCommandResponse result = userAdminService.removeAccount()
        if (result.status != PassFail.PASS) {
            return new NVRRestMethodException(result.error, "/user/removeAccount")
        } else {
            logService.cam.info("removeAccount: success")
            return result.responseObject
        }
    }

    @Secured(['ROLE_CLOUD', 'ROLE_CLIENT'])
    @PostMapping("/hasActiveMQCreds")
    def hasActiveMQCreds() {
        ObjectCommandResponse result = userAdminService.hasActiveMQCreds()

        if (result.status != PassFail.PASS) {
            throw new NVRRestMethodException(result.error, "user/hasActiveMQCreds")
        } else {
            logService.cam.info("hasActiveMQCreds: (= ${result.responseObject}) success")
            return ResponseEntity.ok(result.responseObject)
        }
    }
}
