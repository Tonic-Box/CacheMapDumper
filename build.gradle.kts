plugins {
    id("java")
    id("org.jetbrains.kotlin.plugin.lombok") version "1.6.21"
}

group = "osrs.dev"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // https://mvnrepository.com/artifact/net.runelite/cache
    implementation("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    implementation(group = "net.unethicalite", name = "cache", version = "1.0.20-EXPERIMENTAL")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.formdev:flatlaf:2.4")
    implementation("com.google.guava:guava:31.1-jre")
}

tasks.test {
    useJUnitPlatform()
}