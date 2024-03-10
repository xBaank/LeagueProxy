allprojects {
    group = "io.github.xbaank"
    version = "1.0.0"
}

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    id("maven-publish")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            withType<MavenPublication> {
                groupId = group.toString()
                artifactId = project.name
                version = project.version.toString()
            }
        }
    }
}