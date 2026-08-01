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

    // The test-discovery listener (MockartyDiscoveryListener) is a JUnit
    // Platform TestExecutionListener — it compiles against the launcher
    // API. compileOnly, not api: the launcher is the thing that runs the
    // tests, so it is always on the classpath whenever the listener is
    // actually invoked; we must not drag it onto a consumer's main
    // classpath.
    compileOnly("org.junit.platform:junit-platform-launcher:1.10.2")

    // Allure is detected reflectively at runtime — see
    // AllureMirror.isAllureRuntimeAvailable. End-users do NOT need to
    // depend on it; we only pull it in for our own tests to validate the
    // mirror-mode pipeline against a real Allure runtime.
    testImplementation("io.qameta.allure:allure-java-commons:2.27.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.junit.platform:junit-platform-launcher:1.10.2")
    testImplementation("org.slf4j:slf4j-simple:2.0.12")
}

tasks.test {
    // DiscoveryFixtures' nested classes carry real @Test methods purely so
    // the discovery tree-walk tests can selectClass() them. They are NOT
    // this module's own tests — exclude them from the auto-run so they don't
    // show up as phantom test cases. They are still reachable by explicit
    // selection from DiscoveryManifestAssemblerTest / MockartyDiscoveryListenerTest.
    exclude("**/DiscoveryFixtures*")
}
