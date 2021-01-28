
buildscript {

    extra["minSdkVersion"] = 15
    extra["compileSdkVersion"] = 30
    extra["targetSdkVersion"] = 30

    repositories {
        google()
        mavenCentral()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:4.1.2")
        classpath("io.deepmedia.tools:publisher:0.4.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.21")

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