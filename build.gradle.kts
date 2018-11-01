import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

val junitJupiterVersion = "5.3.1"
val spekVersion = "1.2.1"
val kluentVersion = "1.41"
val slf4jVersion = "1.7.25"
val ktorVersion = "0.9.3"
val prometheusVersion = "0.5.0"
val kafkaVersion = "2.0.0"
val confluentVersion = "4.1.2"

val mainClass = "no.nav.helse.AppKt"

plugins {
    application
    kotlin("jvm") version "1.2.71"
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

kotlin.experimental.coroutines = Coroutines.ENABLE

application {
    mainClassName = "$mainClass"
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("org.slf4j:slf4j-simple:$slf4jVersion")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.30.2")
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("io.prometheus:simpleclient_common:$prometheusVersion")
    compile("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    compile("org.apache.kafka:kafka-streams:$kafkaVersion")

    testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testCompile("org.amshove.kluent:kluent:$kluentVersion")
    testCompile("org.jetbrains.spek:spek-api:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }
    testRuntime("org.jetbrains.spek:spek-junit-platform-engine:$spekVersion") {
        exclude(group = "org.junit.platform")
        exclude(group = "org.jetbrains.kotlin")
    }
}

repositories {
    maven {
        url = URI("https://dl.bintray.com/kotlin/ktor")
    }
    jcenter()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_10
    targetCompatibility = JavaVersion.VERSION_1_10
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "4.10.2"
}
