package com.fussionlabs.gradle

import com.fussionlabs.gradle.tasks.BuildTask
import com.fussionlabs.gradle.tasks.DownloadGoTask
import com.fussionlabs.gradle.tasks.InstallGoTask
import com.fussionlabs.gradle.tasks.TestTask
import com.fussionlabs.gradle.utils.PluginUtils.ext
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.extensions.stdlib.capitalized
import kotlin.jvm.java

class GoPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        // Create plugin extension
        project.extensions.create(GO_PLUGIN_EXTENSION, PluginExtension::class.java)

        // Apply the base plugin
        project.plugins.apply("base")

        // Configure the moduleName
        if (project.ext.moduleName.isEmpty()) {
            project.ext.moduleName = project.name
        }

        project.afterEvaluate {
            val checkTask = project.tasks.getByName("check")
            val assembleTask = project.tasks.getByName("assemble")

            val downloadGoTask = project.tasks.register(GO_DOWNLOAD_TASK, DownloadGoTask::class.java) { downloadTask ->
                downloadTask.group = GO_PLUGIN_GROUP
                downloadTask.description = "Download Golang"
                downloadTask.goVersion.set(project.ext.goVersion)
                downloadTask.defaultGoVersion.set(project.ext.defaultGoVersion)
                downloadTask.rootDir.set(project.rootDir)
            }
            // Setup install task
            project.tasks.register(GO_INSTALL_TASK, InstallGoTask::class.java) { installTask ->
                installTask.group = GO_PLUGIN_GROUP
                installTask.description = "Install Golang"
                installTask.goVersion.set(project.ext.goVersion)
                installTask.defaultGoVersion.set(project.ext.defaultGoVersion)
                installTask.rootDir.set(project.rootDir)

                installTask.dependsOn(downloadGoTask)
            }

            // Setup build tasks
            project.ext.os.forEach { osType ->
                project.ext.arch.forEach { archType ->
                    val task = project.tasks.register("goBuild${osType.capitalized()}${archType.capitalized()}", BuildTask::class.java) { goBuildTask ->
                        goBuildTask.group = GO_PLUGIN_GROUP
                        goBuildTask.description = "Build $osType $archType"

                        // Configure Inputs
                        goBuildTask.os = osType
                        goBuildTask.arch = archType
                        goBuildTask.ldFlagsConfig = project.ext.ldFlags

                        // Configure output
                        goBuildTask.outputBinary = "${project.ext.moduleName}-$osType-$archType"
                    }
                    assembleTask.dependsOn(task)
                }
            }

            // Setup test task
            val testTask = project.tasks.register("test", TestTask::class.java) { testTask ->
                testTask.group = GO_PLUGIN_GROUP
                testTask.description = "Run tests"
            }

            checkTask.dependsOn(testTask)
        }
    }
}
