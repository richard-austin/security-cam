package com.fussionlabs.gradle.utils

import com.fussionlabs.gradle.GO_BINARY
import com.fussionlabs.gradle.GO_SETUP_DIR
import com.fussionlabs.gradle.GRADLE_FILES_DIR
import com.fussionlabs.gradle.PluginExtension
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.*
import java.net.URI

object PluginUtils {
    val Project.ext: PluginExtension
        get() = this.extensions.getByType(PluginExtension::class.java)

    fun Boolean.toInt(): Int {
        return if (this) 1 else 0
    }

    fun binaryExists(binary: String): Boolean {
        val command = ArrayList<String>()
        command.add("which")
        command.add(binary)
        val proc = ProcessBuilder(command)
        val process = proc.start()
        process.waitFor()
        return process.exitValue() == 0
    }

    fun goInstalled(): Boolean {
        return binaryExists(GO_BINARY)
    }

    fun getOs(): String {
        return when {
            Os.isFamily(Os.FAMILY_MAC) -> "darwin"
            Os.isFamily(Os.FAMILY_WINDOWS) -> "windows"
            else -> "linux"
        }
    }

    fun getArch(): String {
        val arch = System.getProperty("os.arch")
        return if (arch == "x86_64") {
            "amd64"
        } else if (arch == "aarch64") {
            "arm64"
        } else {
            arch
        }
    }

    fun downloadFile(url: String, outfile: File) {
        try {
            val binaryInputStream = URI(url).toURL().openStream()
            binaryInputStream.use { inputStream ->
                outfile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: IOException) {
            throw GradleException("There was an issue downloading file. ERROR: $e")
        }
    }

    fun goBinary(goVersion: String, defaultGoVersion: String, rootDir: File): String {
        return if (goInstalled() && goVersion.isEmpty()) {
            GO_BINARY
        } else if (!goInstalled() && goVersion.isEmpty()) {
            "${rootDir}/$GRADLE_FILES_DIR/$GO_SETUP_DIR-$defaultGoVersion/go/bin/$GO_BINARY"
        } else {
            "${rootDir}/$GRADLE_FILES_DIR/$GO_SETUP_DIR-$goVersion/go/bin/$GO_BINARY"
        }
    }

}
