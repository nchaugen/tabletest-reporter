import org.gradle.api.provider.Provider
import org.gradle.process.CommandLineArgumentProvider

buildscript {
    repositories {
        mavenCentral()
        mavenLocal()  // For SNAPSHOT plugin - checked after Central to avoid incomplete artifacts
    }
    dependencies {
        classpath("io.github.nchaugen:tabletest-reporter-gradle-plugin:0.3.3-SNAPSHOT")
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
    // JUnit 5.12 (minimum claimed version)
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.0")

    // TableTest (tabletest-reporter-junit added automatically by plugin)
    testImplementation("io.github.nchaugen:tabletest-junit:0.5.8")
}

tasks.test {
    useJUnitPlatform()

    // Custom expectation pattern for testing
    systemProperty("tabletest.reporter.expectation.pattern", "^Expected.*")

    // Allow intentional test failures
    ignoreFailures = true

    val junitOutputDir: Provider<String> = providers.gradleProperty("junitOutputDir")
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        val outputDir: String? = junitOutputDir.orNull
        if (outputDir == null) {
            emptyList()
        } else {
            listOf("-Djunit.platform.reporting.output.dir=$outputDir")
        }
    })
}

// Configure tabletest-reporter plugin for Markdown output
extensions.configure<io.github.nchaugen.tabletest.gradle.TableTestReporterExtension>("tableTestReporter") {
    format.set("markdown")
}
