package com.fussionlabs.gradle.tasks

import com.fussionlabs.gradle.utils.PluginUtils.ext
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles

@CacheableTask
abstract class TestTask : GoTask() {
    @Classpath
    @InputFiles
    var inputFiles = project.fileTree(project.rootDir)
        .matching{ matchingFile ->
            matchingFile.include("**/**_test.go")
        }

    override fun exec() {
        // Setup build dir
        val buildDir = project.layout.buildDirectory.get().asFile
        buildDir.mkdirs()

        // Configure test args
        val testArgs = mutableListOf("test")

        // Add extraTestArgs (if defined)
        project.ext.extraTestArgs.forEach { testArg ->
            testArgs.add(testArg)
        }

        // Configure Project DIR
        testArgs.add("${project.rootDir}/...")
        goTaskArgs = testArgs

        super.exec()
    }
}
