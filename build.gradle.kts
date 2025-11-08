plugins {
    application
    id("java")
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
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

repositories {
    mavenCentral()
    maven(url = "https://maven.lavalink.dev/releases")
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
    implementation("dev.arbjerg:lavaplayer:2.2.4")
    implementation("dev.lavalink.youtube:common:1.16.0")
}

tasks.test {
    useJUnitPlatform()
}