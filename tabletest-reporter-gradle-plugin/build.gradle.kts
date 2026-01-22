plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "2.0.0"
    id("com.diffplug.spotless") version "8.1.0"
}

group = "io.github.nchaugen"
version = "0.3.2-SNAPSHOT"

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
    testImplementation("org.assertj:assertj-core:3.27.3")
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
            description = "Generate AsciiDoc or Markdown docs from TableTest results"
            tags.set(listOf("testing", "bdd", "documentation", "tabletest", "asciidoc", "markdown"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

spotless {
    java {
        palantirJavaFormat("2.83.0")
        removeUnusedImports()
        importOrder("", "javax", "java", "\\#")
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}
