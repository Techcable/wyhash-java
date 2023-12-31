plugins {
    id("wyhash.java-library-conventions")
    id("com.palantir.git-version") version "3.0.0"
}

val gitVersion: groovy.lang.Closure<String> by extra
version = gitVersion()

extra.set("artifactId", "wyhash")

java {
    withSourcesJar()
    withJavadocJar()
}

base {
    archivesName.set("wyhash-java")
}

tasks.test {
    useJUnitPlatform()
}
