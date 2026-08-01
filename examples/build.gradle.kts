plugins {
    `java`
}

description = "Mockarty Java SDK — runnable examples (compiled by CI so they cannot rot)"

// Examples are documentation that has to keep compiling. This module exists
// so `./gradlew build` fails the moment an example drifts from the SDK API:
// before it was wired in, examples/ was outside settings.gradle.kts and
// accumulated 48 compile errors that nobody saw, because nothing ever built it.
// It is deliberately NOT published — see the absent maven-publish plugin.
// 17, not 11: :mockarty-testcontainers / :mockarty-pact / :mockarty-fuzz are
// themselves 17+, so an 11-target examples module cannot resolve them. The
// published SDK modules keep their own (lower) targets — this is a build-only
// module, so raising it costs consumers nothing.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":mockarty-java"))
    implementation(project(":mockarty-junit5"))
    implementation(project(":mockarty-pact"))
    implementation(project(":mockarty-fuzz"))
    implementation(project(":mockarty-testcontainers"))
    implementation(project(":mockarty-protocols"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
}
