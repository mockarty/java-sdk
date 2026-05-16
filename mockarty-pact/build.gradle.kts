plugins {
    `java-library`
    id("com.vanniktech.maven.publish")
}

description = "Mockarty Pact - Pact V3+V4 consumer DSL for Mockarty (pure JVM, no Rust FFI)"

// Java 17 baseline for this module: we use sealed interfaces + records in
// the matcher domain. The other SDK modules stay on Java 11 — that's fine,
// they don't transitively expose pact types through their public API.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Shared types (jackson ObjectMapper config flavour, exception base, etc.)
    // come from mockarty-java. mockarty-junit5 is needed for the optional
    // JUnit5 extension we ship alongside the DSL — users who only want the
    // raw DSL can still consume the module without touching JUnit.
    api(project(":mockarty-java"))
    compileOnly(project(":mockarty-junit5"))
    compileOnly("org.junit.jupiter:junit-jupiter-api:5.10.2")

    testImplementation(project(":mockarty-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.slf4j:slf4j-simple:2.0.12")
}

// The JUnit5 SPI extension under META-INF/services/.../Extension is
// honoured only when junit-platform sees this system property at startup —
// otherwise users would have to write @ExtendWith everywhere, which
// defeats the auto-discovery contract we ship to consumers.
tasks.withType<Test> {
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
}
