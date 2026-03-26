plugins {
    `java-library`
    id("com.vanniktech.maven.publish")
}

description = "Mockarty JUnit 5 Extension - Test integration for Mockarty mock server"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    api(project(":mockarty-java"))
    api("org.junit.jupiter:junit-jupiter-api:5.10.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.slf4j:slf4j-simple:2.0.12")
}
