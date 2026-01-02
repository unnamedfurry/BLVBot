plugins {
    application
    id("java")
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.bytedeco.gradle-javacpp-platform") version "1.5.10"
}

application {
    mainClass.set("org.unnamedfurry.BotLauncher")
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "org.unnamedfurry.BotLauncher"
    }
}

group = "org.unnamedfurry"
version = "1.0-BETA"
extra["javacppPlatform"] = "linux-x86_64"

repositories {
    mavenCentral()
    maven("https://maven.lavalink.dev/releases")
    maven(url = "https://jitpack.io")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("net.dv8tion:JDA:6.1.1")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("org.json:json:20250517")
    implementation("dev.arbjerg:lavalink-client:3.2.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.16.1")
    implementation("commons-io:commons-io:2.15.1")
    implementation("org.bytedeco:javacv-platform:1.5.12")
}

tasks.test {
    useJUnitPlatform()
}