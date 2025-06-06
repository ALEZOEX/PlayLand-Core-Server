plugins {
    java
    `maven-publish`
    id("io.papermc.paperweight.patcher") version "1.7.4"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        content { onlyForConfigurations("paperclip") }
    }
}

dependencies {
    remapper("net.fabricmc:tiny-remapper:0.10.3:fat")
    decompiler("org.vineflower:vineflower:1.10.1")
    paperclip("io.papermc:paperclip:3.0.3")
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }
}

subprojects {
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 21
    }
    tasks.withType<Javadoc> {
        options.encoding = "UTF-8"
    }
    tasks.withType<ProcessResources> {
        filteringCharset = "UTF-8"
    }

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

paperweight {
    serverProject = project(":playland-server")

    remapRepo = "https://maven.quiltmc.org/repository/release/"
    decompileRepo = "https://maven.quiltmc.org/repository/release/"

    usePaperUpstream(providers.gradleProperty("paperRef")) {
        withPaperPatcher {
            apiPatchDir = layout.projectDirectory.dir("patches/api")
            apiOutputDir = layout.projectDirectory.dir("playland-api")

            serverPatchDir = layout.projectDirectory.dir("patches/server")
            serverOutputDir = layout.projectDirectory.dir("playland-server")
        }
    }
}

tasks.register("printMinecraftVersion") {
    doLast {
        println(providers.gradleProperty("mcVersion").get())
    }
}

tasks.register("printPaperVersion") {
    doLast {
        println(providers.gradleProperty("paperRef").get())
    }
}
