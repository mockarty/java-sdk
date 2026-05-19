plugins {
    `java-library`
    id("com.vanniktech.maven.publish")
}

description = "Mockarty Java SDK — multi-protocol test clients (SOAP, GraphQL, SSE, WebSocket, gRPC, Kafka, RabbitMQ) with auto-step capture"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    // Step capture talks to the same vocabulary the rest of mockarty-java
    // uses for external runs; bring in the core module so the user only
    // has one transitive dep ("ru.mockarty:mockarty-protocols").
    api(project(":mockarty-java"))

    // gRPC: dynamic JSON ↔ protobuf via JsonFormat + descriptor-set / reflection
    // resolution. grpc-netty-shaded keeps the transport self-contained so user
    // CI envs don't need a separate Netty drop.
    api("io.grpc:grpc-stub:1.66.0")
    api("io.grpc:grpc-protobuf:1.66.0")
    api("io.grpc:grpc-services:1.66.0")
    api("io.grpc:grpc-netty-shaded:1.66.0")
    api("com.google.protobuf:protobuf-java:3.25.5")
    api("com.google.protobuf:protobuf-java-util:3.25.5")

    // Kafka producer + consumer. Pinned to 3.7.x because that's the
    // newest line that still supports JDK 11 (4.x bumps the floor to 17).
    api("org.apache.kafka:kafka-clients:3.7.1")

    // RabbitMQ AMQP 0-9-1 client.
    api("com.rabbitmq:amqp-client:5.22.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.slf4j:slf4j-simple:2.0.12")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
