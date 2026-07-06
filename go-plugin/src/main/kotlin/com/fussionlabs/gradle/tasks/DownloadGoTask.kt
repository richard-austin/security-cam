package com.fussionlabs.gradle.tasks

import com.fussionlabs.gradle.GO_SETUP_DIR
import com.fussionlabs.gradle.GRADLE_FILES_DIR
import com.fussionlabs.gradle.utils.PluginUtils
import com.fussionlabs.gradle.utils.PluginUtils.getArch
import com.fussionlabs.gradle.utils.PluginUtils.getOs
import com.fussionlabs.gradle.utils.PluginUtils.goBinary
import com.fussionlabs.gradle.utils.PluginUtils.goInstalled
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class DownloadGoTask @Inject constructor(
    @get:Internal val projectLayout: ProjectLayout
) : DefaultTask() {
    @get:Input
    abstract val goVersion: Property<String>
    @get:Input
    abstract val defaultGoVersion: Property<String>
    @get:Input
    abstract val rootDir: Property<File>

    init {
        onlyIf {
            installGo()
        }
    }

    fun installGo(): Boolean {
        return (!goInstalled() || goVersion.get().isNotEmpty())
    }

    @TaskAction
    fun downloadGo() {
        val buildDir = projectLayout.buildDirectory.get().asFile
        val golangVersion = goVersion.get().ifEmpty {
            defaultGoVersion.get()
        }
        // val goVersion = "1.25.4"
        val url = "https://go.dev/dl/go${golangVersion}.${getOs()}-${getArch()}.tar.gz"
        val outputLocation = "$buildDir/go${golangVersion}.${getOs()}-${getArch()}.tar.gz"

        if (!File(goBinary(goVersion.get(), defaultGoVersion.get(),rootDir.get())).exists()) {
            // Setup the build directory
            buildDir.mkdirs()

            val outputFile = File(outputLocation)
            outputFile.createNewFile()
            val destinationDir = File("${rootDir.get()}/$GRADLE_FILES_DIR/$GO_SETUP_DIR-$golangVersion")
            destinationDir.mkdirs()

            // Download the file
            logger.lifecycle("Downloading Go version $golangVersion")
            logger.info("Source URL: $url")
            PluginUtils.downloadFile(url, outputFile)
            logger.lifecycle("Done")
        }
    }
}
