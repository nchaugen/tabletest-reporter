buildscript {
    repositories {
        mavenCentral()
        mavenLocal()  // For SNAPSHOT plugin - checked after Central to avoid incomplete artifacts
    }
    dependencies {
        classpath("org.tabletest:tabletest-reporter-gradle-plugin:1.0.0-SNAPSHOT")
    }
}

plugins {
    java
    id("io.quarkus") version "3.31.3"
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
    testImplementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.31.3"))

    // Quarkus JUnit 5
    testImplementation("io.quarkus:quarkus-junit5")

    // JUnit Platform Launcher (version managed by Quarkus BOM)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // TableTest (tabletest-reporter-junit added automatically by plugin)
    testImplementation("org.tabletest:tabletest-junit:1.0.0")
}

tasks.test {
    useJUnitPlatform()
}

// Configure tabletest-reporter plugin for AsciiDoc output
extensions.configure<org.tabletest.gradle.TableTestReporterExtension>("tableTestReporter") {
    format.set("asciidoc")
}
