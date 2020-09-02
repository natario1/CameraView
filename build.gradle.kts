
buildscript {

    extra["minSdkVersion"] = 15
    extra["compileSdkVersion"] = 29
    extra["targetSdkVersion"] = 29

    repositories {
        google()
        mavenCentral()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:4.0.1")
        classpath("com.otaliastudios.tools:publisher:0.3.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.0")

    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }
}

tasks.register("clean", Delete::class) {
    delete(buildDir)
}