rootProject.name = "persisted-queue"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            version("android-tools-gradle", "7.2.2")
            version("kotlin", "1.7.20")
            library("kotlinx-coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").version("1.6.4")
            library("kotlinx-serialization", "org.jetbrains.kotlinx", "kotlinx-serialization-json").version("1.4.0")
            library("kotlinx-datetime", "org.jetbrains.kotlinx", "kotlinx-datetime").version("0.4.0")
            library("androidx-test-core", "androidx.test", "core").version("1.4.0")
            library("roboelectric", "org.robolectric", "robolectric").version("4.5.1")
            library("multiplatform-settings", "com.russhwolf", "multiplatform-settings").version("1.0.0-RC")
            plugin("versioning", "net.nemerosa.versioning").version("3.0.0")
            plugin("kotlin.serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin")
        }
    }
}
