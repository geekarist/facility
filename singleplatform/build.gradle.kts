import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"
    id("org.jetbrains.compose") version "1.3.1"
}

group = "me.cpele.workitems"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation("io.ktor:ktor-server-core:2.2.3")
                implementation("io.ktor:ktor-server-netty:2.2.3")
                implementation("com.ngrok:ngrok-api-java:0.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-html:0.8.1")
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.2.1")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.8.1")
                implementation("org.oolong-kt:oolong:2.1.1")
                implementation("com.slack.api:slack-api-client:1.27.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
                implementation("net.harawata:appdirs:1.2.1")
            }
            dependsOn(commonMain)
        }

        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "me.cpele.workitems.shell.MainKt"
        args.add("slack-account")
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "work-items"
            packageVersion = "1.0.0"
            linux {
                iconFile.set(project.file("core/src/main/resources/me/cpele/workitems/core/programs/app-icon.png"))
            }
        }
    }
}
