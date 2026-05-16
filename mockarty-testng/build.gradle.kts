plugins {
    `java-library`
    id("com.vanniktech.maven.publish")
}

description = "Mockarty TestNG Adapter - emits Allure-2 result files + ExternalRuns from TestNG suites"

// Java 17 baseline — consistent with mockarty-pact / mockarty-fuzz /
// mockarty-testcontainers. The adapter uses records + pattern-matching
// in the result-builder paths.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // mockarty-junit5 carries the AllureLifecycle / AllureWriter — both
    // the TestNG and Cucumber adapters re-use the same emitter so a
    // hybrid project produces ONE allure-results/ directory.
    api(project(":mockarty-java"))
    api(project(":mockarty-junit5"))

    // TestNG: provided at compile-time, user pulls the same version they
    // already depend on. Constraint here keeps the API stable.
    compileOnly("org.testng:testng:7.10.2")

    testImplementation("org.testng:testng:7.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.slf4j:slf4j-simple:2.0.12")
}
