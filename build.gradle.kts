import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel

plugins {
    id("java")
    // 2.3.0 to match the local IntelliJ IDEA (261) Kotlin metadata used for the runIde sandbox.
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    // 2.18.0 (not 2.1.0): the pluginVerification DSL is incompatible with Gradle 9.3 on 2.1.0.
    id("org.jetbrains.intellij.platform") version "2.18.0"
}

group = "io.genai.robots"
version = "0.1.3"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Dev/sandbox uses the locally-installed IntelliJ IDEA (so `runIde` launches it); CI/other
// machines download IDEA Community. The plugin uses only platform APIs + java.net.http, so a
// single build loads in every JetBrains IDE.
val ideaApp = file("/Applications/IntelliJ IDEA.app/Contents")
val useLocalIde = ideaApp.exists() && !providers.environmentVariable("CI").isPresent

dependencies {
    intellijPlatform {
        if (useLocalIde) {
            local(ideaApp.absolutePath)
        } else {
            // Must match sinceBuild (242): the split-preview editor API we use is 2024.2+, so
            // compiling against 2024.1 fails. Used by CI (no local IDE) and other machines.
            intellijIdeaCommunity("2024.2.5")
        }
    }
}

// Compile WITH JDK 21 (IntelliJ 261 platform jars are Java-21 bytecode) but EMIT Java 17
// (--release 17) so the plugin still loads on JBR-17 IDEs (sinceBuild 233).
java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }

kotlin {
    jvmToolchain(21)
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // 242 (2024.2): TextEditorWithPreview became a Kotlin class then; its constructors are
            // binary-incompatible with the 233–241 Java-era class (Marketplace verifier: Critical).
            sinceBuild = "242"
            untilBuild = provider { null }
        }
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        failureLevel.set(listOf(
            FailureLevel.COMPATIBILITY_PROBLEMS,
            FailureLevel.INTERNAL_API_USAGES,
            FailureLevel.MISSING_DEPENDENCIES,
            FailureLevel.INVALID_PLUGIN,
        ))
        ides {
            // Verify the floor (242, where breakage would surface) plus the latest IDE.
            select {
                types.set(listOf(IntelliJPlatformType.IntellijIdeaCommunity))
                sinceBuild.set("242")
                untilBuild.set("242.*")
            }
            latest { types.set(listOf(IntelliJPlatformType.IntellijIdeaCommunity)) }
        }
    }
}

// Slow and clashes with a running runIde sandbox; not needed for a dev build.
tasks.named("buildSearchableOptions") { enabled = false }
