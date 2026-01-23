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
    id("io.quarkus") version "3.30.3"
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
    // Quarkus BOM for dependency management
    testImplementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.30.3"))

    // Quarkus JUnit 5
    testImplementation("io.quarkus:quarkus-junit5")

    // JUnit Platform Launcher (version managed by Quarkus BOM)
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

// Configure tabletest-reporter plugin for AsciiDoc output
extensions.configure<io.github.nchaugen.tabletest.gradle.TableTestReporterExtension>("tableTestReporter") {
    format.set("asciidoc")
}
