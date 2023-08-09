plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.20.0")
}

kotlin {
    // NOTE: Need to use a version kotlin understands
    jvmToolchain(17)
}
