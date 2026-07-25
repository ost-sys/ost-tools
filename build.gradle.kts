// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.3.2" apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.built.in1.kotlin) apply false
}

subprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.bouncycastle" && requested.name == "bcprov-jdk18on") {
                useVersion("1.80.2")
                because("Fix Dependabot vulnerability in BouncyCastle GOST CTR mode")
            }
        }
    }
}