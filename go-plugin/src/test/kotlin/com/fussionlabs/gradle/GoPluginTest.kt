package com.fussionlabs.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GoPluginTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun testDefaultTasksExist() {
        val project: Project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply(GoPlugin::class.java)

        // Force project evaluation
        project.getTasksByName("build", false)

        val expectedTasks = listOf("goBuildDarwinAmd64", "goBuildDarwinArm64", "goBuildLinuxAmd64", "goBuildLinuxArm64")
        expectedTasks.forEach { expectedTask ->
            assertNotNull(project.tasks.getByName(expectedTask))
        }
    }
}