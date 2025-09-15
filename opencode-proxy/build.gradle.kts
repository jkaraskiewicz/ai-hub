plugins {
  kotlin("multiplatform") version "2.2.20"
  kotlin("plugin.serialization") version "2.2.20"
}

group = "com.aihub"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

kotlin {
  val hostOs = System.getProperty("os.name")
  val arch = System.getProperty("os.arch")
  val nativeTarget = when {
    hostOs == "Mac OS X" && arch == "x86_64" -> macosX64("native")
    hostOs == "Mac OS X" && arch == "aarch64" -> macosArm64("native")
    hostOs == "Linux" && arch == "x86_64" -> linuxX64("native")
    hostOs == "Linux" && arch == "aarch64" -> linuxArm64("native")
    else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
  }

  nativeTarget.apply {
    binaries {
      executable {
        entryPoint = "main"
      }
    }
  }

  sourceSets {
    nativeMain.dependencies {
      // Ktor server
      implementation("io.ktor:ktor-server-core:3.2.3")
      implementation("io.ktor:ktor-server-cio:3.2.3")

      // Ktor client
      implementation("io.ktor:ktor-client-core:3.2.3")
      implementation("io.ktor:ktor-client-cio:3.2.3")
      implementation("io.ktor:ktor-client-content-negotiation:3.2.3")

      // Ktor server content negotiation
      implementation("io.ktor:ktor-server-content-negotiation:3.2.3")
      implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.3")

      // Serialization
      implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

      // DateTime
      implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.6.x-compat")

      // DI
      implementation("io.insert-koin:koin-core:4.1.0")
      implementation("io.insert-koin:koin-ktor:4.1.0")
    }
  }
}
