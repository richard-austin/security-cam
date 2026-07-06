package com.fussionlabs.gradle.tasks

import com.fussionlabs.gradle.GO_SETUP_DIR
import com.fussionlabs.gradle.GRADLE_FILES_DIR
import com.fussionlabs.gradle.utils.PluginUtils.getArch
import com.fussionlabs.gradle.utils.PluginUtils.getOs
import com.fussionlabs.gradle.utils.PluginUtils.goBinary
import com.fussionlabs.gradle.utils.PluginUtils.goInstalled
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations // Required for tarTree
import org.gradle.api.file.FileSystemOperations // Required for copy
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class InstallGoTask @Inject constructor(
    @get:Internal val projectLayout: ProjectLayout,
    @get:Internal val fileSystemOperations: FileSystemOperations,
    @get:Internal val archiveOperations: ArchiveOperations // Inject ArchiveOperations here
) : DefaultTask() {

    @get:Input
    abstract val goVersion: Property<String>

    @get:Input
    abstract val defaultGoVersion: Property<String>

    @get:Input
    abstract val rootDir: Property<File>

    init {
        group = "go"
        description = "Installs Go"
        onlyIf {
            installGo()
        }
    }
    fun installGo(): Boolean {
        return (!goInstalled() || goVersion.get().isNotEmpty())
    }


    @TaskAction
    fun installGoBinaries() {
        val golangVersion = goVersion.get().ifEmpty {
            defaultGoVersion.get()
        }

        if (!File(goBinary(golangVersion, defaultGoVersion.get(), rootDir.get())).exists()) {
            val buildDir = projectLayout.buildDirectory.get().asFile
            val tarfileLocation = File(buildDir, "go${golangVersion}.${getOs()}-${getArch()}.tar.gz")
            val outputDir = File(rootDir.get(), "$GRADLE_FILES_DIR/$GO_SETUP_DIR-$golangVersion")

            logger.lifecycle("Extracting  ${tarfileLocation.absolutePath} ::::: into ${outputDir.absolutePath}")

            // Perform the extraction during execution phase
            fileSystemOperations.copy { spec ->
                // Use archiveOperations to create the tarTree
                spec.from(archiveOperations.tarTree(tarfileLocation))
                spec.into(outputDir)
            }

            // Delete the tarfile safely after successful extraction
            if (tarfileLocation.exists()) {
                tarfileLocation.delete()
            }
        }
    }
}
