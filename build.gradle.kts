import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

val kafkaVersion = "2.0.1"
val fuelVersion = "1.15.1"

val mockkVersion = "1.8.12.kotlin13"
val junitJupiterVersion = "5.3.1"
val spekVersion = "1.2.1"
val kluentVersion = "1.41"
val wireMockVersion = "2.19.0"
val mainClass = "no.nav.helse.AppKt"

plugins {
    application
    kotlin("jvm") version "1.3.10"
    id("com.github.johnrengelman.shadow") version "4.0.3"
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

application {
    mainClassName = "$mainClass"
}

dependencies {
    compile(kotlin("stdlib"))
    compile("no.nav.helse:streams:10")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.30.2")
    compile("com.github.kittinunf.fuel:fuel:$fuelVersion")

    testImplementation ("no.nav:kafka-embedded-env:2.0.1")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testCompile("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")
    testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testCompile("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testCompile("com.github.tomakehurst:wiremock:$wireMockVersion")

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
    jcenter()
    mavenCentral()
    maven("http://packages.confluent.io/maven/")
    maven("https://dl.bintray.com/kotlin/ktor")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<ShadowJar> {
    classifier = ""
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.0"
}
