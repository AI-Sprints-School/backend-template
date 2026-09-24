plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "career.springreview"
version = "0.0.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(providers.gradleProperty("javaToolchain").getOrElse("17")))
    }
}

repositories {
    maven("https://repo1.maven.org/maven2/")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}
