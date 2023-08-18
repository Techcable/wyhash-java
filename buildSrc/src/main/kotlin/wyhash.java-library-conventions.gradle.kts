import com.diffplug.spotless.LineEnding

plugins {
    `java-library`
    id("com.diffplug.spotless")
    `maven-publish`
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9+")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

group = "net.techcable.algorithms.hash.wyhash"

tasks.withType<JavaCompile> {
    options.release.set(17)
}

val spdxLicenseId = "Apache-2.0 WITH LLVM-Exception"

spotless {
    lineEndings = LineEnding.UNIX

    format("common") {
        target("*")
        targetExclude(
            // Ignore gradle wrapper files
            "gradlew.bat", "gradlew",
            // Ignore eclipse files
            ".classpath", ".project", ".settings/*"
        )

        indentWithSpaces(4)
        endWithNewline()
        trimTrailingWhitespace()
    }
    java {
        /*
         * Our primary code formatter
         *
         * NOTE: Sometimes this can make some style decisions
         * I disagree with. Especially with respect to splitting
         * things across multiple lines.
         *
         * It may require manual override & cleanup,
         * which is why we add toggleOffOn()
         */
        palantirJavaFormat("2.34.0")

        /*
         * Allow selectively disabling formatting
         * with '// spotless:off' and '// spotless:on'
         *
         * This allows manual workarounds for bad
         * formatting decisions from palantirJavaFormat.
         *
         * NOTE: The lack of spaces between
         * 'spotless', ':', and 'off' is needed for the
         * disable comment to work...
         */
        toggleOffOn()

        // Cleanup imports
        importOrder("java|javax", "", "net.techcable", "\\#")

        licenseHeader("// SPDX-License-Identifier: ${spdxLicenseId}\n\n")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "net.techcable.algorithms.hash.wyhash"
            project.afterEvaluate {
                artifactId = checkNotNull(project.extra.get("artifactId")) {
                    "Please specify artifactId"
                } as String
                version = project.version as String
            }

            from(components["java"])

            pom {
                name.set("wyhash-java")
                description.set(provider { project.description })
                scm {
                    url.set("https://github.com/Techcable/wyhash-java")
                    connection.set("scm:git:https://github.com/Techcable/wyhash-java.git")
                }
                issueManagement {
                    system.set("Github Issues")
                    url.set("https://github.com/Techcable/wyhash-java/issues")
                }
                licenses {
                    license {
                        name.set("SPDX Id: $spdxLicenseId")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        distribution.set("repo")
                        comments.set(listOf(
                            "The LLVM exception is included.",
                            "See https://spdx.dev/ids for an explanation of SPDX ids."
                        ).joinToString(" "))
                    }
                }
            }
        }
    }
}
