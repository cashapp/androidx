import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.kotlin.parcelize) apply false
  alias(libs.plugins.kotlin.native.cocoapods) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.android.application) apply false
}

allprojects {
  group = "app.cash.paging"
  version = "${rootProject.libs.versions.androidx.paging.get()}-0.2.0-SNAPSHOT"

  repositories {
    mavenCentral()
    google()
  }

  plugins.withId("com.vanniktech.maven.publish.base") {
    configure<PublishingExtension> {
      repositories {
        maven {
          name = "testMaven"
          url = file("${rootProject.buildDir}/testMaven").toURI()
        }
      }
    }
    @Suppress("UnstableApiUsage")
    configure<MavenPublishBaseExtension> {
      publishToMavenCentral(SonatypeHost.DEFAULT)
      signAllPublications()
      pom {
        description.set("Packages AndroidX's Paging library for Kotlin/Multiplatform.")
        name.set(project.name)
        url.set("https://github.com/cashapp/multiplatform-paging/")
        licenses {
          license {
            name.set("The Apache Software License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            distribution.set("repo")
          }
        }
        developers {
          developer {
            id.set("cashapp")
            name.set("Cash App")
          }
        }
        scm {
          url.set("https://github.com/cashapp/multiplatform-paging/")
          connection.set("scm:git:https://github.com/cashapp/multiplatform-paging.git")
          developerConnection.set("scm:git:ssh://git@github.com/cashapp/multiplatform-paging.git")
        }
      }
    }
  }
}
