package security.cam.commands

import grails.validation.Validateable
import org.springframework.web.multipart.MultipartFile

class UploadMaskFileCommand implements Validateable {
    MultipartFile maskFile

    String toString() {
        return ("\n" +
                """
                filename	= ${maskFile.originalFilename}
                """)
    }

    static constraints = {
        maskFile(validator: { maskFile, obj ->
            if (maskFile == null) {
                return false
            }
            if (maskFile.empty) {
                return false
            }
        })
    }
}
