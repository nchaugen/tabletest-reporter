buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath("io.github.nchaugen:tabletest-reporter-gradle-plugin:0.3.2-SNAPSHOT")
    }
}

plugins {
    java
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
}

apply(plugin = "io.github.nchaugen.tabletest-reporter")

group = "io.github.nchaugen.tabletest.compat"
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

    // TableTest
    testImplementation("io.github.nchaugen:tabletest-junit:0.5.8")

    // TableTest Reporter JUnit Extension
    testImplementation("io.github.nchaugen:tabletest-reporter-junit:0.3.2-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()

    // Enable JUnit extension autodetection
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")

    // Configure JUnit Platform output directory for publishFile() API
    // JUnit 6+ prepends the engine ID (junit-jupiter) to the path, so use build directory as base
    val outputDir = layout.buildDirectory
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Djunit.platform.reporting.output.dir=${outputDir.get().asFile.absolutePath}")
    }
}

// Configure tabletest-reporter plugin for Markdown output
// Supports property overrides: -Ptabletest.inputDir=... -Ptabletest.outputDir=...
extensions.configure<io.github.nchaugen.tabletest.gradle.TableTestReporterExtension>("tableTestReporter") {
    format.set("markdown")
    if (project.hasProperty("tabletest.inputDir")) {
        inputDir.set(file(project.property("tabletest.inputDir") as String))
    }
    if (project.hasProperty("tabletest.outputDir")) {
        outputDir.set(file(project.property("tabletest.outputDir") as String))
    }
}
