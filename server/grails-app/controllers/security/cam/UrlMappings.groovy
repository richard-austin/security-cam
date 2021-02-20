package security.cam

class UrlMappings {

    static mappings = {
//        delete "/$controller/$id(.$format)?"(action:"delete")
//        get "/$controller(.$format)?"(action:"index")
//        get "/$controller/$id(.$format)?"(action:"show")
//        post "/$controller(.$format)?"(action:"save")
//        put "/$controller/$id(.$format)?"(action:"update")
//        patch "/$controller/$id(.$format)?"(action:"patch")

//        "/"(controller: 'application', action:'index')
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

//        "/login/$action?"(controller: "login")
//        "/logout/$action?"(controller: "logout")

        "/"(uri: '/index.html')
        "500"(view: '/error')
        "404"(view: '/notFound')
    }
}
