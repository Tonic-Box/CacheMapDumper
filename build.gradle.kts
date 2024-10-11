plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    java
}

group = "osrs.dev"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.runelite.net")
    }
    mavenCentral()
}

val TaskContainer.publishToMavenLocal: TaskProvider<DefaultTask>
    get() = named<DefaultTask>("publishToMavenLocal")

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    implementation(group = "net.runelite", name = "cache", version = "latest.release")
    //implementation(group = "net.unethicalite", name = "cache", version = "1.0.20-EXPERIMENTAL")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.formdev:flatlaf:2.4")
    implementation("com.google.guava:guava:32.0.0-android")
    implementation("org.apache.commons:commons-collections4:4.3")
    implementation("commons-beanutils:commons-beanutils:1.9.4")
    implementation("org.apache.commons:commons-configuration2:2.10.1")
}


tasks {
    register<Copy>("copyCompileDeps") {
        into("./build/deps/")
        from(configurations["compileClasspath"])
    }

    build {
        finalizedBy("shadowJar")
    }
    jar {
        manifest {
            attributes(mutableMapOf("Main-Class" to "osrs.dev.Main"))
        }
    }
    shadowJar {
        archiveClassifier.set("shaded")
        isZip64 = true
    }
}