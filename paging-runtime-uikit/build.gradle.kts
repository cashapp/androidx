import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.mavenPublish)
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
  targetHierarchy.default()

  ios()
  iosSimulatorArm64()

  sourceSets {
    all {
      languageSettings {
        optIn("androidx.paging.ExperimentalPagingApi")
        optIn("kotlin.RequiresOptIn")
      }
    }
    val commonMain by getting {
      dependencies {
        api(projects.pagingCommon)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.stately.concurrency)
        implementation(libs.stately.iso.collections)
      }
    }
    val iosMain by getting {
      kotlin.srcDir("../upstreams/androidx-main/paging/paging-runtime/src/commonMain/kotlin")
    }
  }
}

configure<MavenPublishBaseExtension> {
  configure(KotlinMultiplatform(javadocJar = JavadocJar.Empty()))
}
