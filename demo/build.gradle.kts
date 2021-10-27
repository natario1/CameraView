plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = property("compileSdkVersion") as Int
    defaultConfig {
        applicationId = "com.otaliastudios.cameraview.demo"
        minSdk = property("minSdkVersion") as Int
        targetSdk = property("targetSdkVersion") as Int
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
    }
    sourceSets["main"].java.srcDir("src/main/kotlin")
}

dependencies {
    implementation(project(":cameraview"))
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("com.google.android.material:material:1.4.0")
}
