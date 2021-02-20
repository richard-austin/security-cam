// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.userLookup.userDomainClassName = 'com.securitycam.User'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'com.securitycam.UserRole'
grails.plugin.springsecurity.authority.className = 'com.securitycam.Role'
grails.plugin.springsecurity.controllerAnnotations.staticRules = [
	[pattern: '/',               access: ['ROLE_CLIENT']],
	[pattern: '/error',          access: ['ROLE_CLIENT']],
	[pattern: '/index',          access: ['ROLE_CLIENT']],
	[pattern: '/index.gsp',      access: ['ROLE_CLIENT']],
	[pattern: '/shutdown',       access: ['ROLE_CLIENT']],
	[pattern: '/assets/**',      access: ['ROLE_CLIENT']],
	[pattern: '/**/js/**',       access: ['ROLE_CLIENT']],
	[pattern: '/**/css/**',      access: ['ROLE_CLIENT']],
	[pattern: '/**/images/**',   access: ['ROLE_CLIENT']],
	[pattern: '/**/index.html',  access: ['ROLE_CLIENT']],
	[pattern: '/#/**',           access: ['ROLE_CLIENT']],
	[pattern: '/**/*.js',        access: ['ROLE_CLIENT']],
    [pattern: '/**/*.css',       access: ['ROLE_CLIENT']],
	[pattern: '/**/favicon.ico', access: ['permitAll']]
]

grails.plugin.springsecurity.filterChain.chainMap = [
	[pattern: '/assets/**',      filters: 'none'],
	[pattern: '/**/js/**',       filters: 'none'],
	[pattern: '/**/css/**',      filters: 'none'],
	[pattern: '/**/images/**',   filters: 'none'],
	[pattern: '/**/favicon.ico', filters: 'none'],
	[pattern: '/**',             filters: 'JOINED_FILTERS']
]
