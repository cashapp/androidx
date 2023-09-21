import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
  targetHierarchy.default()

  listOf(
    iosArm64(),
    iosSimulatorArm64(),
    iosX64(),
  ).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "shared"
    }
  }
  jvm()

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.pagingCommon)
        api(libs.kotlinx.coroutines.core)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.contentNegotiation)
        implementation(libs.ktor.serialization.kotlinx.json)
      }
    }
    val iosMain by getting {
      dependencies {
        api(projects.pagingRuntimeUikit)
        implementation(libs.ktor.client.darwin)
      }
    }
    val iosSimulatorArm64Main by getting {
      dependsOn(iosMain)
      dependencies {
        api(libs.kotlinx.coroutines.core.iossimulatorarm64)
      }
    }
    val jvmMain by getting {
      dependencies {
        implementation(libs.ktor.client.okhttp)
      }
    }
  }
}
