buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath("io.github.nchaugen:tabletest-reporter-gradle-plugin:0.2.1-SNAPSHOT")
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
    // JUnit 6 (latest)
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.1")

    // TableTest
    testImplementation("io.github.nchaugen:tabletest-junit:0.5.8")

    // TableTest Reporter JUnit Extension
    testImplementation("io.github.nchaugen:tabletest-reporter-junit:0.2.1-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()

    // Enable JUnit extension autodetection
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
}

// Configure tabletest-reporter plugin for Markdown output
extensions.configure<io.github.nchaugen.tabletest.gradle.TableTestReporterExtension>("tableTestReporter") {
    format.set("markdown")
}
