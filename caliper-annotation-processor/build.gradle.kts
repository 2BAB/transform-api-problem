plugins {
    kotlin("jvm")
}

dependencies {
    api(projects.caliperAnnotation)
    implementation(deps.kotlin.std)
    implementation(deps.kotlin.serialization)
    implementation(deps.apache.common.text)
    implementation(deps.ksp.api)
    implementation(deps.javapoet)
    implementation(deps.kotlinpoet)
    implementation(deps.kotlinpoet.interop.javapoet)
    implementation(deps.kotlinpoet.interop.ksp)

    testImplementation(deps.hamcrest)
    testImplementation(deps.mockk)
    testImplementation(deps.kotlin.compile.testing)
    testImplementation(deps.kotlin.compile.testing.ksp)
    testImplementation(gradleTestKit())
}