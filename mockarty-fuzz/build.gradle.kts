plugins {
    `java-library`
    id("com.vanniktech.maven.publish")
}

description = "Mockarty Fuzz - language-side fuzz target DSL that transpiles to Mockarty's canonical fuzz JSON config (no embedded fuzz engine)"

// Java 17 baseline: this module uses sealed interfaces + records in the
// Assertion / Seed / Result / Finding domain. The same rationale as
// mockarty-pact — these types never escape this module's public API into
// the Java 11 modules, so the bump is local.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // mockarty-java carries the shared Jackson ObjectMapper config and the
    // existing FuzzingApi / FuzzingConfig types we transpile to. The DSL
    // does NOT depend on any external fuzzer — by design (thin-layer rule).
    api(project(":mockarty-java"))
    // JUnit5 extension is optional; consumers who only want the raw DSL
    // can drop the @Test-engine dependency without losing the core API.
    compileOnly(project(":mockarty-junit5"))
    compileOnly("org.junit.jupiter:junit-jupiter-api:5.10.2")

    testImplementation(project(":mockarty-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    // Launcher promoted to testImplementation: the extension test drives
    // JUnit programmatically (LauncherFactory + SummaryGeneratingListener)
    // to verify the @MockartyFuzz/@FuzzBuilder auto-wired flow end-to-end.
    testImplementation("org.junit.platform:junit-platform-launcher:1.10.2")
    testImplementation("org.slf4j:slf4j-simple:2.0.12")
}

// Same SPI auto-discovery contract the pact module uses — users only
// write @MockartyFuzz, no @ExtendWith needed.
tasks.withType<Test> {
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
}
