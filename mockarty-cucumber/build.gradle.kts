plugins {
    `java-library`
    id("com.vanniktech.maven.publish")
}

description = "Mockarty Cucumber-JVM Adapter - emits Allure-2 result files + ExternalRuns from Cucumber feature runs"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api(project(":mockarty-java"))
    api(project(":mockarty-junit5"))

    // Cucumber: provided. User pulls a compatible 7.x release.
    compileOnly("io.cucumber:cucumber-java:7.15.0")
    compileOnly("io.cucumber:cucumber-plugin:7.15.0")

    testImplementation("io.cucumber:cucumber-java:7.15.0")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.15.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.slf4j:slf4j-simple:2.0.12")
}
