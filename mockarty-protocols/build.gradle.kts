plugins {
    `java-library`
    id("com.vanniktech.maven.publish")
}

description = "Mockarty Java SDK — multi-protocol test clients (SOAP, GraphQL, SSE, WebSocket) with auto-step capture"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    // Step capture talks to the same vocabulary the rest of mockarty-java
    // uses for external runs; bring in the core module so the user only
    // has one transitive dep ("ru.mockarty:mockarty-protocols").
    api(project(":mockarty-java"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.slf4j:slf4j-simple:2.0.12")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
