package com.fussionlabs.gradle

open class PluginExtension {
    var moduleName:String = ""
    var cgoEnabled:Boolean = false
    var os:List<String> = listOf("linux", "darwin")
    var arch:List<String> = listOf("arm64", "amd64")
    var goVersion:String = ""
    var defaultGoVersion:String = "1.21.6"
    var ldFlags:Map<String, String> = mapOf()
    var extraBuildArgs:List<String> = listOf()
    var extraTestArgs:List<String> = listOf()
}