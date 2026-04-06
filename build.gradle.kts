plugins {
    kotlin("jvm") version "2.2.0" apply false
    id("com.vanniktech.maven.publish") version "0.35.0" apply false
}

allprojects {
    group = "ru.mockarty"
    version = "0.2.1"

    repositories {
        mavenCentral()
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
