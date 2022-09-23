plugins {
    id("com.android.library") version libs.versions.android.tools.gradle
    kotlin("multiplatform") version libs.versions.kotlin
    alias(libs.plugins.versioning)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
    id("signing")
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    ios {
        binaries.framework()
    }

    iosSimulatorArm64()

    android {
        publishLibraryVariants("debug", "release")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines)
                implementation(libs.kotlinx.serialization)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val androidMain by getting
        val androidTest by getting {
            dependencies {
                implementation(libs.roboelectric)
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation(libs.androidx.test.core)
            }
        }
        val iosMain by getting
        val iosTest by getting
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
        val iosSimulatorArm64Test by getting {
            dependsOn(iosTest)
        }
        all {
            languageSettings {
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
                optIn("kotlin.RequiresOptIn")
            }
        }
    }
}

android {
    compileSdk = 30

    defaultConfig {
        minSdk = 21
        targetSdk = 30
        testInstrumentationRunner = "androidx.test.runner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests.apply {
            isReturnDefaultValues = true
        }
    }
}

group = "com.liftric"
version = with(versioning.info) {
    if (branch == "HEAD" && dirty.not()) tag else full
}

afterEvaluate {
    project.publishing.publications.withType(MavenPublication::class.java).forEach {
        it.groupId = group.toString()
    }
}

val ossrhUsername: String? by project
val ossrhPassword: String? by project

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    repositories {
        maven {
            name = "sonatype"
            setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
        }
    }

    publications.withType<MavenPublication> {
        artifact(javadocJar.get())

        pom {
            name.set(project.name)
            description.set("Kotlin Multiplatform persisted queue library.")
            url.set("https://github.com/liftric/cognito-idp")

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://github.com/liftric/cognito-idp/blob/master/LICENSE")
                }
            }
            developers {
                developer {
                    id.set("gaebel")
                    name.set("Jan Gaebel")
                    email.set("gaebel@liftric.com")
                }
            }
            scm {
                url.set("https://github.com/liftric/persisted-queue")
            }
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}
