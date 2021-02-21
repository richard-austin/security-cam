package server

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

 //       "/"(view:"/index")
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(uri: '/index.html')
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
