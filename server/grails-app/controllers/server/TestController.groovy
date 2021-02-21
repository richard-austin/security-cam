package server

import grails.plugin.springsecurity.annotation.Secured

class TestController {
	static responseFormats = ['json', 'xml']

    @Secured(['ROLE_CLIENT'])
    def index() {
        render "This is the test controller"
    }
}
