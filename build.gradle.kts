plugins {
    application
    id("java")
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass.set("com.unnamedfurry.BotLauncher")
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "com.unnamedfurry.BotLauncher"
    }
}

group = "org.unnamedfurry"
version = "1.0-BETA"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("net.dv8tion:JDA:6.1.1")
    implementation("org.glassfish:javax.json:1.1.4")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("org.slf4j:slf4j-api:2.0.16")
}

tasks.test {
    useJUnitPlatform()
}