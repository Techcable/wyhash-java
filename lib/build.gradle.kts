import com.diffplug.spotless.LineEnding

plugins {
    `java-library`
    id("com.diffplug.spotless") version "6.20.0"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly("org.jetbrains:annotations:24.0.1")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}


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
    }
}
