
buildscript {

    extra["minSdkVersion"] = 15
    extra["compileSdkVersion"] = 31
    extra["targetSdkVersion"] = 31

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31")
        classpath("org.jfrog.buildinfo:build-info-extractor-gradle:4.25.1")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(buildDir)
}