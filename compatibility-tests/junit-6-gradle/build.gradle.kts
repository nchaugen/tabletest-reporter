buildscript {
    repositories {
        mavenCentral()
        mavenLocal()  // For SNAPSHOT plugin - checked after Central to avoid incomplete artifacts
    }
    dependencies {
        classpath("io.github.nchaugen:tabletest-reporter-gradle-plugin:0.4.1-SNAPSHOT")
    }
}

plugins {
    java
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
    // JUnit 6.0 (latest)
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")

    // TableTest (tabletest-reporter-junit added automatically by plugin)
    testImplementation("org.tabletest:tabletest-junit:1.0.0")
}

tasks.test {
    useJUnitPlatform()

    // Custom expectation pattern for testing
    systemProperty("tabletest.reporter.expectation.pattern", "^Expected.*")

    // Allow intentional test failures
    ignoreFailures = true
}

val reporterInputDir: String? = providers.gradleProperty("tabletestReporterInputDir").orNull

// Configure tabletest-reporter plugin for AsciiDoc output
extensions.configure<io.github.nchaugen.tabletest.gradle.TableTestReporterExtension>("tableTestReporter") {
    format.set("asciidoc")
    if (reporterInputDir != null) {
        inputDir.set(project.layout.projectDirectory.dir(reporterInputDir))
    }
}
