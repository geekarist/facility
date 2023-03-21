plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}
dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.oolong-kt:oolong:2.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
}
repositories {
    mavenCentral()
}