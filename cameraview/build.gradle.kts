import io.deepmedia.tools.publisher.common.License
import io.deepmedia.tools.publisher.common.Release
import io.deepmedia.tools.publisher.common.GithubScm

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("io.deepmedia.tools.publisher")
    id("jacoco")
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
    buildTypes["debug"].isTestCoverageEnabled = true
    buildTypes["release"].isMinifyEnabled = false
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

// Publishing

publisher {
    project.description = "A well documented, high-level Android interface that makes capturing " +
            "pictures and videos easy, addressing all of the common issues and needs. " +
            "Real-time filters, gestures, watermarks, frame processing, RAW, output of any size."
    project.artifact = "cameraview"
    project.group = "com.otaliastudios"
    project.url = "https://github.com/natario1/CameraView"
    project.scm = GithubScm("natario1", "CameraView")
    project.addLicense(License.APACHE_2_0)
    project.addDeveloper("natario1", "mat.iavarone@gmail.com")
    release.sources = Release.SOURCES_AUTO
    release.docs = Release.DOCS_AUTO
    release.version = "2.7.2"

    directory()

    sonatype {
        auth.user = "SONATYPE_USER"
        auth.password = "SONATYPE_PASSWORD"
        signing.key = "SIGNING_KEY"
        signing.password = "SIGNING_PASSWORD"
    }

    sonatype("snapshot") {
        repository = io.deepmedia.tools.publisher.sonatype.Sonatype.OSSRH_SNAPSHOT_1
        release.version = "latest-SNAPSHOT"
        auth.user = "SONATYPE_USER"
        auth.password = "SONATYPE_PASSWORD"
        signing.key = "SIGNING_KEY"
        signing.password = "SIGNING_PASSWORD"
    }
}

// Code Coverage
val buildDir = project.buildDir.absolutePath
val coverageInputDir = "$buildDir/coverage_input" // changing? change github workflow
val coverageOutputDir = "$buildDir/coverage_output" // changing? change github workflow

// Run unit tests, with coverage enabled in the android { } configuration.
// Output will be an .exec file in build/jacoco.
tasks.register("runUnitTests") { // changing name? change github workflow
    dependsOn("testDebugUnitTest")
    doLast {
        copy {
            from("$buildDir/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
            into("$coverageInputDir/unit_tests") // changing? change github workflow
        }
    }
}

// Run android tests with coverage.
tasks.register("runAndroidTests") { // changing name? change github workflow
    dependsOn("connectedDebugAndroidTest")
    doLast {
        copy {
            from("$buildDir/outputs/code_coverage/debugAndroidTest/connected")
            include("*coverage.ec")
            into("$coverageInputDir/android_tests") // changing? change github workflow
        }
    }
}

// Merge the two with a jacoco task.
jacoco { toolVersion = "0.8.5" }
tasks.register("computeCoverage", JacocoReport::class) {
    dependsOn("compileDebugSources") // Compile sources, needed below
    executionData.from(fileTree(coverageInputDir))
    sourceDirectories.from(android.sourceSets["main"].java.srcDirs)
    additionalSourceDirs.from("$buildDir/generated/source/buildConfig/debug")
    additionalSourceDirs.from("$buildDir/generated/source/r/debug")
    classDirectories.from(fileTree("$buildDir/intermediates/javac/debug") {
        // Not everything here is relevant for CameraView, but let's keep it generic
        exclude(
                "**/R.class",
                "**/R$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "android/**",
                "androidx/**",
                "com/google/**",
                "**/*\$ViewInjector*.*",
                "**/Dagger*Component.class",
                "**/Dagger*Component\$Builder.class",
                "**/*Module_*Factory.class",
                // We don"t test OpenGL filters.
                "**/com/otaliastudios/cameraview/filters/**.*"
        )
    })
    reports.html.required.set(true)
    reports.xml.required.set(true)
    reports.html.outputLocation.set(file("$coverageOutputDir/html"))
    reports.xml.outputLocation.set(file("$coverageOutputDir/xml/report.xml"))
}