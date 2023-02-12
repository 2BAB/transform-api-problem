plugins {
    id("com.android.application")
    kotlin("android")

    id("me.2bab.caliper")
}

android {
    namespace = "me.xx2bab.caliper.sample"
    compileSdk = 31
    defaultConfig {
        applicationId = "me.xx2bab.caliper.sample"
        minSdk = 23
        targetSdk = 31
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    sourceSets["main"].java.srcDir("src/main/kotlin")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(deps.kotlin.std)
    implementation("androidx.appcompat:appcompat:1.4.1")

    implementation(project(":library"))

    caliper(project(":custom-proxy"))
}
