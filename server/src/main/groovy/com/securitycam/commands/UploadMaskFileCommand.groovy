package com.securitycam.commands

import jakarta.validation.constraints.NotNull
import org.springframework.web.multipart.MultipartFile

class UploadMaskFileCommand {
    @NotNull
    MultipartFile maskFile

    String toString() {
        return ("\n" +
                """
                filename	= ${maskFile.originalFilename}
                """)
    }
}
