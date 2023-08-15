plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    kotlin("plugin.serialization")
}
repositories {
    mavenCentral()
}
dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.oolong-kt:oolong:2.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
}
