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
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.1")

    // TableTest
    testImplementation("io.github.nchaugen:tabletest-junit:0.5.8")

    // TableTest Reporter JUnit Extension
    testImplementation("io.github.nchaugen:tabletest-reporter-junit:0.3.2-SNAPSHOT")

    // JSoup for HTML CSS verification
    testImplementation("org.jsoup:jsoup:1.22.1")
}

tasks.test {
    useJUnitPlatform()

    // Enable JUnit extension autodetection
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")

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
