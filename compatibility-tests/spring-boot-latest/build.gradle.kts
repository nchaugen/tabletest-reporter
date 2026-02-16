buildscript {
    repositories {
        mavenCentral()
        mavenLocal()  // For SNAPSHOT plugin - checked after Central to avoid incomplete artifacts
    }
    dependencies {
        classpath("org.tabletest:tabletest-reporter-gradle-plugin:1.0.1-SNAPSHOT")
    }
}

plugins {
    java
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
}

apply(plugin = "org.tabletest.reporter")

group = "org.tabletest.compat"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Spring Boot Test Starter (includes JUnit)
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // JUnit Platform Launcher (required for Gradle test runner with JUnit 6)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // TableTest (tabletest-reporter-junit added automatically by plugin)
    testImplementation("org.tabletest:tabletest-junit:1.0.0")
}

tasks.test {
    useJUnitPlatform()
}

// Configure tabletest-reporter plugin for Markdown output
extensions.configure<org.tabletest.gradle.TableTestReporterExtension>("tableTestReporter") {
    format.set("markdown")
}
