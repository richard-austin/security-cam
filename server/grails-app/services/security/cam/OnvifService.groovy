package security.cam

import grails.gorm.transactions.Transactional
import security.cam.onvif.OnvifManager
import security.cam.onvif.models.Device

@Transactional
class OnvifService {
    OnvifManager onvifManager = new OnvifManager()

    def getMediaProfiles(Device device) {

    }
}
