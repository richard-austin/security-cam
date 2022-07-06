package spring

import com.proxy.CloudProxyProperties
import grails.core.GrailsApplication
import grails.util.Environment
import security.cam.commands.UserPasswordEncoderListener

import security.cam.eventlisteners.SecCamAuthFailEventListener
import security.cam.eventlisteners.SecCamSecurityEventListener
import security.cam.eventlisteners.TwoFactorAuthenticationProvider
import security.cam.interfaceobjects.TwoFactorAuthenticationDetailsSource

// Place your Spring DSL code here
beans = {
    //GrailsApplication grailsApplication

    userPasswordEncoderListener(UserPasswordEncoderListener)

    // This bean audits user logins and logouts
    secCamSecurityEventListener(SecCamSecurityEventListener) {
        logService = ref("logService")
    }

    // This bean audits failed user logins
    secCamAuthFailEventListener(SecCamAuthFailEventListener) {
        logService = ref("logService")
    }

    restfulProperties(CloudProxyProperties) {
        grailsApplication = ref('grailsApplication')
    }

    if (grailsApplication.config.grails.plugin.springsecurity.active == true) {
        twoFactorAuthenticationProvider(TwoFactorAuthenticationProvider) {
            logService = ref("logService")
            userDetailsService = ref('userDetailsService')
            passwordEncoder = ref('passwordEncoder')
            userCache = ref('userCache')
            preAuthenticationChecks = ref('preAuthenticationChecks')
            postAuthenticationChecks = ref('postAuthenticationChecks')
            authoritiesMapper = ref('authoritiesMapper')
            hideUserNotFoundExceptions = true
        }

        authenticationDetailsSource(TwoFactorAuthenticationDetailsSource)
    }
}
