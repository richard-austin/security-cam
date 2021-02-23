package server

import grails.core.GrailsApplication
import grails.plugin.springsecurity.annotation.Secured
import grails.plugins.GrailsPluginManager
import grails.plugins.PluginManagerAware

class ApplicationController implements PluginManagerAware {

    GrailsApplication grailsApplication
    GrailsPluginManager pluginManager

    @Secured(['ROLE_CLIENT'])
    def index() {
        [grailsApplication: grailsApplication, pluginManager: pluginManager]
    }
}
