plugins {
    `java-library`
    id("com.vanniktech.maven.publish")
}

description = "Mockarty Java SDK - Core client library for Mockarty mock server"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    api("com.fasterxml.jackson.core:jackson-annotations:2.17.0")
    api("com.fasterxml.jackson.core:jackson-core:2.17.0")
    api("org.slf4j:slf4j-api:2.0.12")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.slf4j:slf4j-simple:2.0.12")
}
