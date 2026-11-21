import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()
    js {
        useEsModules() // required by MapLibre GL JS
        browser()
        binaries.executable()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    android {
       namespace = "ch.lauzhand.helpinghand.app.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    sourceSets {
        // The real map (MapLibre) exists on Android and web-JS only; the source is shared by both.
        androidMain { kotlin.srcDir("src/mapLibreShared/kotlin") }
        jsMain { kotlin.srcDir("src/mapLibreShared/kotlin") }
        androidMain.dependencies {
            implementation(libs.maplibre.compose)
            runtimeOnly(libs.maplibre.compose.runtimeVulkanAndroid)
            implementation(libs.ktor.clientOkhttp)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            api(project(":core"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // HTTP client for RemoteRepository
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientContentNegotiationCommon)
            implementation(libs.ktor.serializationJsonCommon)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.clientOkhttp)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.clientJs)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jsMain.dependencies {
            implementation(libs.maplibre.compose)
            implementation(libs.ktor.clientJs)
            implementation(libs.wrappers.browser)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.testRunner)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}