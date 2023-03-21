plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.oolong-kt:oolong:2.1.1")
}
repositories {
    mavenCentral()
}