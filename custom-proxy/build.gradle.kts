plugins {
    id("com.android.library")
    kotlin("android")

    id("com.google.devtools.ksp")
}

android {
    namespace = "me.xx2bab.caliper.sample.customproxy"
    compileSdk = 31
    defaultConfig {
        minSdk = 23
        targetSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    implementation(projects.caliperAnnotation)
    ksp(projects.caliperAnnotationProcessor)
}

