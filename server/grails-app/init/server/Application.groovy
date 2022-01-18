package server

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration

import groovy.transform.CompileStatic
import org.springframework.boot.web.servlet.ServletContextInitializer

import javax.servlet.ServletContext
import javax.servlet.ServletException

@CompileStatic
class Application extends GrailsAutoConfiguration implements ServletContextInitializer {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    @Override
    void onStartup(ServletContext servletContext) throws ServletException {
        servletContext.getSessionCookieConfig().setName("NVRSESSIONID")
    }
}
