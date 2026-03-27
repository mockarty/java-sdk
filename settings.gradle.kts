pluginManagement {
    plugins {
        kotlin("jvm") version "2.2.0"
        id("com.vanniktech.maven.publish") version "0.35.0"
    }
}

rootProject.name = "mockarty-sdk"
include("mockarty-java", "mockarty-junit5", "mockarty-kotlin")
