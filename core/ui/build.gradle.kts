import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.bosankus.officeworks.core.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
        androidResources { enable = true }
        // Gives commonTest a compilation (otherwise Kotlin warns it's unused) and a home for future tests.
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            // Only for DataError, so every feature maps errors to messages the same way.
            implementation(projects.core.domain)
            // Exposed so features build on the same theme and components.
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.ui)
            api(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.coil.compose)
            implementation(libs.coil.networkKtor)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.bosankus.officeworks.core.ui.resources"
    generateResClass = always
}
