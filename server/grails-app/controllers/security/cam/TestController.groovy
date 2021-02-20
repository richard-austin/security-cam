package security.cam

import grails.plugin.springsecurity.annotation.Secured
import grails.rest.*
import grails.converters.*

class TestController {
	static responseFormats = ['json', 'xml']

    @Secured(['ROLE_CLIENT'])
    def index() {
        render "This is the test controller"
    }
}
