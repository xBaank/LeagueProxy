import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
}

val ktor_version: String by project
val kotlinProcessVersion: String by project

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
            implementation("io.ktor:ktor-network:$ktor_version")
            implementation("io.ktor:ktor-network-tls:$ktor_version")
            implementation("io.ktor:ktor-server-websockets:$ktor_version")
            implementation("io.ktor:ktor-server-cio:$ktor_version")
            implementation("io.ktor:ktor-client-core:$ktor_version")
            implementation("io.ktor:ktor-client-cio:$ktor_version")
            implementation("io.ktor:ktor-client-websockets:$ktor_version")
            implementation("io.ktor:ktor-client-encoding:$ktor_version")
            implementation("io.ktor:ktor-server-cors:$ktor_version")
            implementation("io.ktor:ktor-server-sessions:$ktor_version")
            implementation("io.github.reactivecircus.cache4k:cache4k:0.13.0")
            implementation("io.arrow-kt:arrow-core:1.2.1")
            implementation("io.github.xbaank:simpleJson-core:3.0.0")
            implementation("ru.gildor.coroutines:kotlin-coroutines-okhttp:1.0")
            // https://mvnrepository.com/artifact/org.yaml/snakeyaml
            implementation("org.yaml:snakeyaml:2.0")
            implementation("com.squareup.okio:okio:3.3.0")
            // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
            implementation("com.squareup.okhttp3:okhttp:4.11.0")
            // https://mvnrepository.com/artifact/io.insert-koin/koin-core
            implementation("io.insert-koin:koin-core:3.5.3")
            // https://mvnrepository.com/artifact/io.insert-koin/koin-compose
            implementation("io.insert-koin:koin-compose:1.1.2")

            // Check the 🔝 maven central badge 🔝 for the latest $kotlinProcessVersion
            implementation("com.github.pgreze:kotlin-process:$kotlinProcessVersion")
            implementation(project(":Rtmp"))
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

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.unmaskedLeague"
            packageVersion = "1.0.0"
        }
    }
}