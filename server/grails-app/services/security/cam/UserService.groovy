package security.cam


import grails.gorm.services.Service
import security.cam.User

@Service(User)
interface UserService {

    User findByUsername(String username)

    User save(User user)
}
