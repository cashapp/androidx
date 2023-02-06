rootProject.name = "multiplatform-paging-root"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
  }
}

include(":paging-common")
include(":paging-runtime-composeui")
include(":paging-runtime-uikit")
include(":samples:repo-search:shared")
include(":samples:repo-search:shared-composeui")
include(":samples:repo-search:android-composeui")
include(":samples:repo-search:desktop-composeui")
