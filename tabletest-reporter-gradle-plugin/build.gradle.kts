plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "2.0.0"
}

group = "io.github.nchaugen"
version = "0.1.1-SNAPSHOT"

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
    // Core library (install to mavenLocal first via: mvn -q -DskipTests install)
    implementation("io.github.nchaugen:tabletest-reporter-core:${project.version}")

    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.1")
}

gradlePlugin {
    website.set("https://github.com/nchaugen/tabletest-reporter")
    vcsUrl.set("https://github.com/nchaugen/tabletest-reporter.git")

    plugins {
        create("tabletestReporter") {
            id = "io.github.nchaugen.tabletest-reporter"
            implementationClass = "io.github.nchaugen.tabletest.gradle.TableTestReporterPlugin"
            displayName = "TableTest Reporter"
            description = "Generate AsciiDoc or Markdown docs from TableTest YAML outputs"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    repositories {
        mavenLocal()
    }
}
