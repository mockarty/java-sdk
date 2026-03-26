plugins {
    `java-library`
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

description = "Mockarty Kotlin DSL - Kotlin extensions and DSL for Mockarty mock server"

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":mockarty-java"))
    implementation(kotlin("stdlib"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.slf4j:slf4j-simple:2.0.12")
}
