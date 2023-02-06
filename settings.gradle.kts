rootProject.name = "multiplatform-paging-root"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
}

include(":paging-common")
include(":paging-runtime-uikit")
include(":samples:repo-search:shared")
include(":samples:repo-search:androidApp")
