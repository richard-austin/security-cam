import security.cam.commands.UserPasswordEncoderListener
import security.cam.eventlisteners.SecCamAuthFailEventListener
import security.cam.eventlisteners.SecCamSecurityEventListener


// Place your Spring DSL code here
beans = {
    userPasswordEncoderListener(UserPasswordEncoderListener)

    // This bean audits user logins and logouts
    secCamSecurityEventListener(SecCamSecurityEventListener) {
        logService = ref("logService")
    }

    // This bean audits failed user logins
    secCamAuthFailEventListener(SecCamAuthFailEventListener) {
        logService = ref("logService")
    }
}