package security.cam


import grails.gorm.services.Service
import security.cam.Role

@Service(Role)
interface RoleService {
    Role save(String authority)
    Role findByAuthority(String authority)
}