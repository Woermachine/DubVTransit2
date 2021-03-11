import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

buildscript {
    repositories {
        mavenLocal()
        maven("https://kotlin.bintray.com/kotlinx")
        maven("https://dl.bintray.com/jetbrains/kotlin-native-dependencies")
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://plugins.gradle.org/m2/")
        google()
        jcenter()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.31")
        classpath("com.android.tools.build:gradle:3.5.0")
        classpath("co.touchlab:kotlinxcodesync:0.1.5")
    }
}

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization") version "1.4.30"
    id("com.squareup.sqldelight")
}

allprojects {
    repositories {
        mavenLocal()
        google()
        jcenter()
        mavenCentral()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://kotlin.bintray.com/ktor")
        maven("https://kotlin.bintray.com/kotlinx")
    }
}

apply(plugin = "maven-publish")
apply(plugin = "com.android.library")
apply(plugin = "kotlin-android-extensions")

sqldelight {
    database("AppDatabase") {
        packageName = "com.stephenwoerner.dubvtransittwo"
    }
}

kotlin {
    android()
    ios {
        binaries {
            framework {
                baseName = "shared"
            }
        }
    }
    sourceSets {
        val commonMain by getting
        val androidMain by getting
        val iosMain by getting
    }
    sourceSets["commonMain"].dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
        implementation("io.ktor:ktor-client-core:1.5.2")
//        implementation("io.ktor:ktor-client-gson:1.5.2")
        implementation("io.ktor:ktor-client-serialization:1.5.2")
        implementation("com.soywiz.korlibs.klock:klock:1.12.0") //https://github.com/korlibs/klock
        implementation("com.squareup.sqldelight:runtime:1.4.4")
        implementation(kotlin("stdlib-common"))
    }
    sourceSets["androidMain"].dependencies {
        //dependency to platform part of kotlinx.coroutines will be added automatically
        implementation("com.squareup.sqldelight:android-driver:1.4.4")
        implementation(kotlin("stdlib-jdk8"))

        implementation("com.google.maps:google-maps-services:0.15.0")
        implementation("com.google.maps.android:maps-ktx:3.0.0")
        implementation("com.google.maps.android:maps-utils-ktx:3.0.0")
    }
    sourceSets["iosX64Main"].dependencies {
        //SQLDelight will be available only in the iOS source set, but not in Android or common
        implementation("com.squareup.sqldelight:native-driver:1.4.4")
    }
}

android {
    compileSdkVersion(30)
    defaultConfig {
        minSdkVersion(26)
        targetSdkVersion(30)
    }
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

val packForXcode by tasks.creating(Sync::class) {
    group = "build"
    val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
    val sdkName = System.getenv("SDK_NAME") ?: "iphonesimulator"
    val targetName = "ios" + if (sdkName.startsWith("iphoneos")) "Arm64" else "X64"
    val framework =
        kotlin.targets.getByName<KotlinNativeTarget>(targetName).binaries.getFramework(mode)
    inputs.property("mode", mode)
    dependsOn(framework.linkTask)
    val targetDir = File(buildDir, "xcode-frameworks")
    from({ framework.outputDirectory })
    into(targetDir)
}
tasks.getByName("build").dependsOn(packForXcode)