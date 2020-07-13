import com.otaliastudios.tools.publisher.common.License
import com.otaliastudios.tools.publisher.common.Release

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("com.otaliastudios.tools.publisher")
    id("jacoco")
}

android {
    setCompileSdkVersion(rootProject.property("compileSdkVersion") as Int)
    defaultConfig {
        setMinSdkVersion(rootProject.property("minSdkVersion") as Int)
        setTargetSdkVersion(rootProject.property("targetSdkVersion") as Int)
        versionCode = 1
        versionName = "2.6.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArgument("filter", "" +
                "com.otaliastudios.cameraview.tools.SdkExcludeFilter," +
                "com.otaliastudios.cameraview.tools.SdkIncludeFilter")
    }
    buildTypes["debug"].isTestCoverageEnabled = true
    buildTypes["release"].isMinifyEnabled = false
}

dependencies {
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-inline:2.28.2")

    androidTestImplementation("androidx.test:runner:1.2.0")
    androidTestImplementation("androidx.test:rules:1.2.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.1")
    androidTestImplementation("org.mockito:mockito-android:2.28.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.2.0")

    api("androidx.exifinterface:exifinterface:1.1.0")
    api("androidx.lifecycle:lifecycle-common:2.1.0")
    api("com.google.android.gms:play-services-tasks:17.0.0")
    implementation("androidx.annotation:annotation:1.1.0")
    implementation("com.otaliastudios.opengl:egloo:0.5.2")
}

// Publishing

publisher {
    project.description = "A well documented, high-level Android interface that makes capturing " +
            "pictures and videos easy, addressing all of the common issues and needs. " +
            "Real-time filters, gestures, watermarks, frame processing, RAW, output of any size."
    project.artifact = "cameraview"
    project.group = "com.otaliastudios"
    project.url = "https://github.com/natario1/CameraView"
    project.addLicense(License.APACHE_2_0)
    release.setSources(Release.SOURCES_AUTO)
    release.setDocs(Release.DOCS_AUTO)
    bintray {
        auth.user = "BINTRAY_USER"
        auth.key = "BINTRAY_KEY"
        auth.repo = "BINTRAY_REPO"
    }
    directory {
        directory = "build/local"
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
            from("$buildDir/jacoco/testDebugUnitTest.exec")
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
jacoco { toolVersion = "0.8.1" }
tasks.register("computeCoverage", JacocoReport::class) {
    dependsOn("compileDebugSources") // Compile sources, needed below
    executionData.from(fileTree(coverageInputDir))
    sourceDirectories.from(android.sourceSets["main"].java.sourceFiles)
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
    reports.html.isEnabled = true
    reports.xml.isEnabled = true
    reports.html.destination = file("$coverageOutputDir/html")
    reports.xml.destination = file("$coverageOutputDir/xml/report.xml")
}