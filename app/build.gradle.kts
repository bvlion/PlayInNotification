plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

val releaseKeystoreFile = rootProject.file("release.jks")

android {
  namespace = "net.ambitious.android.playinnotification"
  compileSdk = 37

  defaultConfig {
    applicationId = "net.ambitious.android.playinnotification"
    minSdk = 31
    targetSdk = 37
    versionCode = 5
    versionName = "1.0.0"
  }

  val releaseSigningConfig = if (releaseKeystoreFile.exists()) {
    signingConfigs.create("release") {
      storeFile = releaseKeystoreFile
      storePassword = System.getenv("KEYSTORE_PASSWORD")
      keyAlias = System.getenv("KEYSTORE_ALIAS")
      keyPassword = System.getenv("KEYSTORE_PASSWORD")
    }
  } else {
    null
  }

  buildTypes {
    release {
      signingConfig = releaseSigningConfig
      optimization {
        enable = true
      }
    }
  }

  buildFeatures {
    buildConfig = true
    compose = true
  }

  testOptions {
    unitTests.isIncludeAndroidResources = true
  }

  sourceSets {
    getByName("test") {
      resources.directories.add("src/main/assets")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
}
