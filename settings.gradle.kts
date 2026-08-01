pluginManagement {
    plugins {
        kotlin("jvm") version "2.2.0"
        id("com.vanniktech.maven.publish") version "0.35.0"
    }
}

// foojay-resolver lets Gradle auto-provision the JDK 17 toolchain that
// :mockarty-kotlin pins. Without this, contributors on JDK 18+ would have
// to install JDK 17 manually before the Kotlin module compiles. The plugin
// is settings-scoped per the Gradle Toolchains contract.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "mockarty-sdk"
include(
    "mockarty-java",
    "mockarty-junit5",
    "mockarty-kotlin",
    "mockarty-pact",
    "mockarty-fuzz",
    "mockarty-testcontainers",
    "mockarty-testng",
    "mockarty-cucumber",
    "mockarty-protocols",
    // Examples are built like any other module so they cannot silently rot —
    // they sat outside this list and accumulated 48 compile errors unnoticed.
    "examples",
)
