plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
    id("com.jfrog.artifactory")
}

android {
    compileSdk = property("compileSdkVersion") as Int
    defaultConfig {
        minSdk = property("minSdkVersion") as Int
        targetSdk = property("targetSdkVersion") as Int
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["filter"] = "" +
                "com.otaliastudios.cameraview.tools.SdkExcludeFilter," +
                "com.otaliastudios.cameraview.tools.SdkIncludeFilter"
    }
    buildTypes["release"].isMinifyEnabled = false

    lint {
        isAbortOnError = false
    }
}

fun Project.getStringProperty(propertyName: String): String? {
    val property = findProperty(propertyName)
    return if(property is String) property else null
}

group = "com.otaliastudios.cameraview"
version = getStringProperty("libraryVersion") ?: "unspecified"

publishing {
    publications {
        register<MavenPublication>("apkRelease") {
            groupId = getStringProperty("groupId")
            version = getStringProperty("libraryVersion")
            artifactId = getStringProperty("artifactId")
            artifact("$buildDir/outputs/aar/${project.name}-release.aar")
        }
    }
}

artifactory {
    setContextUrl("https://premise.jfrog.io/premise")
    publish {
        repository {
            setRepoKey("android-artifacts")
            setUsername(System.getenv("ARTIFACTORY_USERNAME"))
            setPassword(System.getenv("ARTIFACTORY_PASSWORD"))
        }

        defaults {
            publications("apkRelease")
            setPublishArtifacts(true)
            setPublishPom(false)
        }
    }
}


dependencies {
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.mockito:mockito-inline:2.28.2")

    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test:rules:1.4.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("org.mockito:mockito-android:2.28.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

    api("androidx.exifinterface:exifinterface:1.3.3")
    api("androidx.lifecycle:lifecycle-common:2.3.1")
    api("com.google.android.gms:play-services-tasks:17.2.1")
    implementation("androidx.annotation:annotation:1.2.0")
    implementation("com.otaliastudios.opengl:egloo:0.6.1")
}

tasks.register("runUnitTests") { // changing name? change github workflow
    dependsOn("testDebugUnitTest")
}

// Run android tests with coverage.
tasks.register("runAndroidTests") { // changing name? change github workflow
    dependsOn("connectedDebugAndroidTest")
}