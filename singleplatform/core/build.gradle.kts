plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    kotlin("plugin.serialization")
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
repositories {
    mavenCentral()
}
dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.oolong-kt:oolong:2.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
}
