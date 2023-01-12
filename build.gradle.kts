import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

plugins {
    id("com.android.library") version libs.versions.android.tools.gradle
    kotlin("multiplatform") version libs.versions.kotlin
    alias(libs.plugins.versioning)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
    id("signing")
}

group = "com.liftric"
version = with(versioning.info) {
    if (branch == "HEAD" && dirty.not()) tag else full
}

kotlin {
    ios()
    iosSimulatorArm64()

    android {
        publishLibraryVariants("debug", "release")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines)
                implementation(libs.kotlinx.serialization)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.atomicfu)
                implementation(libs.multiplatform.settings)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.multiplatform.settings.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val androidMain by getting
        val androidTest by getting {
            dependencies {
                implementation(libs.roboelectric)
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.ext)

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
    compileSdk = 33

    namespace = "com.liftric.job.queue"

    defaultConfig {
        minSdk = 21
        targetSdk = 33
        testInstrumentationRunner = "androidx.test.runner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
    publishing {
        multipleVariants {
            withSourcesJar()
            withJavadocJar()
            allVariants()
        }
    }
}

tasks {
    withType<KotlinNativeSimulatorTest> {
        deviceId = "iPhone 14"
    }
    withType(JavaCompile::class) {
        options.release.set(11)
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
            name = "GitHubPackages"
            setUrl("https://maven.pkg.github.com/Liftric/kmm-job-queue")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications.withType<MavenPublication> {
        artifact(javadocJar.get())

        pom {
            name.set(project.name)
            description.set("Persistable coroutine job queue for Kotlin Multiplatform projects.")
            url.set("https://github.com/Liftric/kmm-job-queue")

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://github.com/Liftric/kmm-job-queue/blob/master/LICENSE")
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
                url.set("https://github.com/Liftric/kmm-job-queue")
            }
        }
    }
}

afterEvaluate {
    project.publishing.publications.withType(MavenPublication::class.java).forEach {
        it.groupId = group.toString()
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}
