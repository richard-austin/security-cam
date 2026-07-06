# Go Plugin
## Overview
The `Go-Plugin` is a Gradle plugin for Go projects. This plugin does not intend to replace Go's native dependency management system, 
instead this plugin focuses on replacing traditional task orchestrators like Make, offering a more versatile and reusable approach for task automation.

## Usage
Add the following to apply the plugin to your project:

**Groovy DSL**:
```groovy
plugins {
    id "com.fussionlabs.gradle.go-plugin" version "$version"
}
```

**Kotling DSL**:
```kotlin
plugins {
    id("com.fussionlabs.gradle.go-plugin") version("$version")
}
```

## Tasks
The go-plugin offers the following built-in tasks:
```
Go tasks
--------
goBuildDarwinAmd64 - Build darwin amd64 binary
goBuildDarwinArm64 - Build darwin arm64 binary
goBuildLinuxAmd64 - Build linux amd64 binary
goBuildLinuxArm64 - Build linux arm64 binary
installGo - Install Golang (will only run if Go is not installed locally or if goVersion is defined)
test - Run tests
```

## Configuration
The plugin can be easily configured using an extension with the following customizable fields:

| Field Name       | Type                  | Description                                               | Default Value               |
|------------------|-----------------------|-----------------------------------------------------------|-----------------------------|
| `cgoEnabled`     | `Boolean`             | Enable or disable the `CGO_ENABLED` option for builds.    | `false`                     |
| `os`             | `List<String>`        | Specify the target Operating Systems for builds.          | `listOf("linux", "darwin")` |
| `arch`           | `List<String>`        | Specify the target Architectures for builds.              | `listOf("arm64", "amd64")`  |
| `ldFlags`        | `Map<String, String>` | Set custom ldflags for use during builds.                 | `mapOf<String, String>()`   |
| `goVersion`      | `String`              | Go version to install                                     | `1.21.6`                    |
| `extraBuildArgs` | `List<String>`        | Extra build arguments to pass to `goBuild$Os$Arch` tasks. | `listOf<String>()`          |
| `extraTestArgs`  | `List<String>`        | Extra test arguments to `test` task.                      | `listOf<String>()`          |

### Example Configuration
```kotlin
go {
    cgoEnabled = true
    os = listOf("linux")
    arch = listOf("amd64")
    goVersion = "1.20.13"
    ldFlags = mapOf("key1" to "value1", "key2" to "value2")
    extraBuildArgs = listOf("arg1", "arg2")
    extraTestArgs = listOf("arg3", "arg4")
}
```

## Custom Tasks
In addition to the default tasks, you can create custom Go tasks for basically any Go command:
```kotlin
tasks.register("goVersion", com.fussionlabs.gradle.tasks.GoTask::class.java) {
    goTaskArgs = mutableListOf("version")
}
```