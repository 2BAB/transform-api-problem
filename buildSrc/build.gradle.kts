plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // GitHub Release
    implementation("com.android.tools.build:gradle:7.4.0")
    implementation("com.github.breadmoirai:github-release:2.4.1")
    implementation("org.ow2.asm:asm:9.4")
    implementation("org.ow2.asm:asm-util:9.4")
}

gradlePlugin {
    plugins {
        register("me.2bab.caliper") {
            id = "me.2bab.caliper"
            implementationClass ="CaliperPlugin"
            displayName = "Caliper Gradle Plugin"
        }
    }
}

