---
layout: page
title: "Install"
description: "Integrate in your project"
order: 1
---

The library works on API 15+, which is the only requirement and should be met by most projects nowadays.

It is publicly hosted on [Maven Central](https://repo.maven.apache.org/maven2/com/otaliastudios/cameraview), where you
can download the AAR package. To fetch with Gradle, make sure you add the Maven Central repository:

```kotlin
repositories {
  mavenCentral()
}
```

Then simply download the latest version:

```kotlin
api("com.otaliastudios:cameraview:{{ site.github_version }}")
```

No other configuration steps are needed. If you want to try features that were not released yet,
you can pull the latest snapshot by adding the Sonatype snapshot repository:

```kotlin
repositories {
  maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}
```

And depending on the latest-SNAPSHOT version:

```kotlin
api("com.otaliastudios:cameraview:latest-SNAPSHOT")
```