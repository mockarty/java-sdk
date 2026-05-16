plugins {
    `java-library`
    id("com.vanniktech.maven.publish")
}

description = "Mockarty Testcontainers - JVM wrapper around mockarty/cli:latest-mock for integration tests"

// Java 17 baseline: we use records + pattern-matching for the option-
// builder validation paths. Aligns with mockarty-pact; mockarty-java
// and mockarty-junit5 stay on Java 11 — they don't transitively pull
// types from here.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Shared types (jackson + exception base) come from mockarty-java.
    api(project(":mockarty-java"))

    // testcontainers-java is the public surface: callers extend or
    // embed MockartyContainer, so they need GenericContainer on the
    // compile classpath. License: MIT.
    api("org.testcontainers:testcontainers:1.19.7")

    // JUnit5 extension is OPTIONAL — users who only want the raw
    // container don't need to pull JUnit. compileOnly + a separate
    // META-INF/services SPI registration mirrors the mockarty-pact
    // module layout.
    compileOnly(project(":mockarty-junit5"))
    compileOnly("org.junit.jupiter:junit-jupiter-api:5.10.2")

    testImplementation(project(":mockarty-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.slf4j:slf4j-simple:2.0.12")
}

// Auto-discover the JUnit5 extension we ship under
// META-INF/services/.../Extension — same trick the pact module uses.
tasks.withType<Test> {
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
}
