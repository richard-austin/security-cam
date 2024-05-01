// secCamSecurityEventListener is included so that login/logout events get handled.
// secCamAuthFailEventListener deals with failed login attempts
grails.plugin.springsecurity.logout.handlerNames = [
        'rememberMeServices',
        'securityContextLogoutHandler',
        'secCamSecurityEventListener',
        'secCamAuthFailEventListener']

grails.plugin.springsecurity.providerNames = [
        'twoFactorAuthenticationProvider',
        'rememberMeAuthenticationProvider']

//This is needed to turn-on the generation of springsecurity events so that logins and logouts may be audited
grails.plugin.springsecurity.useSecurityEventListener = true

// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.userLookup.userDomainClassName = 'security.cam.User'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'security.cam.UserRole'
grails.plugin.springsecurity.authority.className = 'security.cam.Role'
grails.plugin.springsecurity.controllerAnnotations.staticRules = [
        [pattern: '/', access: ['permitAll']],
        [pattern: '/error', access: ['permitAll']],
        [pattern: '/index', access: ['permitAll']],
        [pattern: '/shutdown', access: ['permitAll']],
        [pattern: '/assets/**', access: ['permitAll']],
        [pattern: '/**/js/**', access: ['permitAll']],
        [pattern: '/**/css/**', access: ['permitAll']],
        [pattern: '/**/images/**', access: ['permitAll']],
        [pattern: '/**/favicon.ico', access: ['permitAll']],
        [pattern: '/user/createOrUpdateAccountLocally', access: ['permitAll']],
        [pattern: '/user/checkForAccountLocally', access: ['permitAll']],
        [pattern: '/user/checkForActiveMQCreds', access: ['permitAll']],
        [pattern: '/user/addOrUpdateActiveMQCreds', access: ['permitAll']],
        [pattern: '/utils/setupSMTPClientLocally', access: ['permitAll']],
        [pattern: '/utils/getSMTPClientParamsLocally', access: ['permitAll']],
        [pattern: '/recover/sendResetPasswordLink', access: ['permitAll']],
        [pattern: '/recover/forgotPassword', access: ['permitAll']],
        [pattern: '/recover/resetPasswordForm', access: ['permitAll']],
        [pattern: '/recover/resetPassword', access: ['permitAll']],
        [pattern: '/**/index.html', access: ['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST']],
        [pattern: '/#/**', access: ['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST']],
        [pattern: '/**/*.js', access: ['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST']],
        [pattern: '/**/*.css', access: ['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST']],
        [pattern: '/**/*.ttf', access: ['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST']],
        [pattern: '/**/stomp/**', access: ['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST']],
        [pattern: '/**/audio/**', access: ['ROLE_CLIENT', 'ROLE_CLOUD']],
        [pattern: '/**/*.woff2', access: ['ROLE_CLIENT', 'ROLE_CLOUD', 'ROLE_GUEST']]
]

grails.plugin.springsecurity.filterChain.chainMap = [
        [pattern: '/assets/**', filters: 'none'],
        [pattern: '/**/js/**', filters: 'none'],
        [pattern: '/**/css/**', filters: 'none'],
        [pattern: '/**/images/**', filters: 'none'],
        [pattern: '/**/favicon.ico', filters: 'none'],
        [pattern: '/**', filters: 'JOINED_FILTERS']
]

