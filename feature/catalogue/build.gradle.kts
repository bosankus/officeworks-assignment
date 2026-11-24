import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.bosankus.officeworks.feature.catalogue"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
        androidResources { enable = true }
        withHostTest {}
        withDeviceTestBuilder {}.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.ui)
            // Direct dependency, otherwise the resources plugin skips generating this module's Res class.
            implementation(libs.compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.koin.core)
            implementation(libs.koin.composeViewmodel)
            implementation(libs.compose.uiToolingPreview)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.mockk)
        }
        getByName("androidDeviceTest").dependencies {
            // Device tests don't include commonTest, so they need kotlin-test (JUnit flavour) themselves.
            implementation(libs.kotlin.testJunit)
            implementation(libs.androidx.testRunner)
            implementation(libs.androidx.testExt.junit)
            implementation(libs.compose.uiTest)
            implementation(libs.androidx.compose.uiTestJunit4)
            implementation(libs.androidx.compose.uiTestManifest)
            // Compose UI test pulls in an older Espresso that crashes on API 34+ (removed InputManager.getInstance).
            implementation(libs.androidx.espresso.core)
        }
    }
}

compose.resources {
    packageOfResClass = "com.bosankus.officeworks.feature.catalogue.resources"
    generateResClass = always
}
