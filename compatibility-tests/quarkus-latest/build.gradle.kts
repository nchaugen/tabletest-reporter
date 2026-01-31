buildscript {
    repositories {
        mavenCentral()
        mavenLocal()  // For SNAPSHOT plugin - checked after Central to avoid incomplete artifacts
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

// Workaround for Guice classifier resolution issue with Quarkus
// See: https://github.com/google/guice/issues/1505
configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("com.google.inject:guice"))
            .using(module("com.google.inject:guice:5.1.0"))
            .withoutClassifier()
    }
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
}

// Configure tabletest-reporter plugin for AsciiDoc output
extensions.configure<io.github.nchaugen.tabletest.gradle.TableTestReporterExtension>("tableTestReporter") {
    format.set("asciidoc")
}
