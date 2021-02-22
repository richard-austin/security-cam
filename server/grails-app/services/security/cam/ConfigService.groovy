package security.cam

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.stereotype.Component
import lombok.Data

@Data
@Component
@ConfigurationProperties(prefix = "cameras")
public class Cameras extends ArrayList<Camera>{
    @Data
    public static class Camera
    {
        String name
        @NestedConfigurationProperty
        ArrayList<uri> uris

        @Data
        public static class uri
        {
            String type
            String uri
        }
    }
}

@Transactional
class ConfigService {
    @Autowired
    private Cameras cameras

    def getCameras() {
        return cameras
    }
}
