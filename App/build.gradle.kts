import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    id("maven-publish")
}

val ktor_version: String by project
val kotlinProcessVersion: String by project
val kotlin_version: String by project

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.material3)
            implementation("org.jetbrains.kotlin:kotlin-scripting-common:$kotlin_version")
            implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:$kotlin_version")
            implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:$kotlin_version")
            implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:$kotlin_version")
            implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven:$kotlin_version")
            implementation("io.github.reactivecircus.cache4k:cache4k:0.13.0")
            implementation("io.arrow-kt:arrow-core:1.2.1")
            implementation("io.github.xbaank:simpleJson-core:3.0.0")
            implementation("ru.gildor.coroutines:kotlin-coroutines-okhttp:1.0")
            implementation("br.com.devsrsouza.compose.icons:font-awesome:1.1.0")
            implementation("io.ktor:ktor-client-core:$ktor_version")
            implementation("io.ktor:ktor-client-encoding:$ktor_version")
            implementation("io.ktor:ktor-network:$ktor_version")
            implementation("com.github.pgreze:kotlin-process:$kotlinProcessVersion")
            implementation("io.github.oshai:kotlin-logging:6.0.3")
            // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
            implementation("org.slf4j:slf4j-api:2.0.12")
            implementation("org.slf4j:slf4j-simple:2.0.12")
            // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-scripting-common

            implementation("com.darkrockstudios:mpfilepicker:3.1.0")
            implementation("io.github.irgaly.kfswatch:kfswatch:1.0.0")
            // https://mvnrepository.com/artifact/org.yaml/snakeyaml
            implementation("org.yaml:snakeyaml:2.0")
            implementation("com.squareup.okio:okio:3.3.0")
            // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
            implementation("com.squareup.okhttp3:okhttp:4.11.0")
            // https://mvnrepository.com/artifact/io.insert-koin/koin-core
            implementation("io.insert-koin:koin-core:3.5.3")
            // https://mvnrepository.com/artifact/io.insert-koin/koin-compose
            implementation("io.insert-koin:koin-compose:1.1.2")
            implementation(project(":Rtmp"))
            implementation(project(":Shared"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "League Proxy"
            packageVersion = version.toString()
        }
    }
}
