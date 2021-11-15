val kotlinVersion: String by project
val kotlinWrappersVersion: String by project
val ktorVersion: String by project
val logbackVersion: String by project
val serializationVersion: String by project

plugins {
    kotlin("multiplatform") version "1.5.31"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5.31"
    application
}

group = "org.omgcobra"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        withJava()
    }
    js(LEGACY) {
        binaries.executable()
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.2.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("ch.qos.logback:logback-classic:$logbackVersion")

                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-auth:$ktorVersion")
                implementation("io.ktor:ktor-html-builder:$ktorVersion")
                implementation("io.ktor:ktor-locations:$ktorVersion")
                implementation("io.ktor:ktor-server-host-common:$ktorVersion")
                implementation("io.ktor:ktor-serialization:$ktorVersion")
                implementation("io.ktor:ktor-websockets:$ktorVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")

                implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")

                implementation("org.jetbrains.exposed:exposed-core:0.35.1")
                implementation("org.jetbrains.exposed:exposed-dao:0.35.1")
                implementation("org.jetbrains.exposed:exposed-jdbc:0.35.1")

                implementation("org.postgresql:postgresql:42.2.2")

                implementation("com.zaxxer:HikariCP:3.4.2")
            }
        }
        val jvmTest by getting
        val jsMain by getting {
            fun kotlinw(target: String) = "org.jetbrains.kotlin-wrappers:kotlin-$target"
            dependencies {
                implementation("io.ktor:ktor-client-js:$ktorVersion")
                implementation("io.ktor:ktor-client-json-js:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization-js:$ktorVersion")

                implementation(project.dependencies.enforcedPlatform(kotlinw("wrappers-bom:$kotlinWrappersVersion")))
                implementation(kotlinw("react"))
                implementation(kotlinw("react-dom"))
                implementation(kotlinw("react-router-dom"))
                implementation(kotlinw("styled"))

                implementation("com.ccfraser.muirwik:muirwik-components:0.9.0")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

application {
    mainClass.set("org.omgcobra.ApplicationKt")
}

tasks.getByName<Jar>("jvmJar") {
    val webpackTask = tasks.getByName<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>(when {
        project.hasProperty("isProduction") -> "jsBrowserProductionWebpack"
        else -> "jsBrowserDevelopmentWebpack"
    })

    dependsOn(webpackTask)
    from(File(webpackTask.destinationDirectory, webpackTask.outputFileName))
}

/*
tasks.named<Copy>("jvmProcessResources") {
    val jsBrowserDistribution = tasks.named("jsBrowserDistribution")
    from(jsBrowserDistribution)
}
*/

tasks.named<JavaExec>("run") {
    val jvmJar = tasks.named<Jar>("jvmJar")

    dependsOn(jvmJar)
    classpath(jvmJar)
}

distributions {
    main {
        contents {
            from("$buildDir/libs") {
                rename("${rootProject.name}-jvm", rootProject.name)
                into("lib")
            }
        }
    }
}

tasks.create("stage") {
    dependsOn("installDist")
}
