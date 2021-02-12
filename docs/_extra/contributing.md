---
layout: page
title: "Contributing & License"
order: 2
---

Everyone is welcome to contribute with suggestions or pull requests, as the library is under active development,
although it has reached a high level of stability.

We are grateful to anyone who has contributed with fixes, features or feature requests. If you don't
want to get involved but still want to support the project, please [consider donating](donate).

### Bug reports

Please make sure to fill the bug report issue template on GitHub.
We highly recommend to try to reproduce the bug in the demo app, as this helps a lot in debugging
and excludes programming errors from your side.

Make sure to include:

- A clear and concise description of what the bug is
- CameraView version, device type, Android API level
- Exact steps to reproduce the issue
- Description of the expected behavior

Recommended extras:

- Screenshots
- LogCat logs (use `CameraLogger.setLogLevel(LEVEL_VERBOSE)` to print all)
- Link to a GitHub repo where the bug is reproducible

### Pull Requests

Please open an issue first.

Unless your PR is a simple fix (typos, documentation, bugs with obvious solution), opening an issue
will let us discuss the problem, take design decisions and have a reference to the issue description.

Please write tests.

Unless the code was already not covered by tests, updated tests are required for merging. The lib
has a few unit tests and more robust tests in the `androidTest` folder, which can be run by Android Studio.

### License

CameraView was formally born as a fork of [CameraKit-Android](https://github.com/wonderkiln/CameraKit-Android) 
and [Google's CameraView](https://github.com/google/cameraview), but has been completely rewritten since.

CameraKit's source code is licensed under the [MIT](https://github.com/wonderkiln/CameraKit-Android/blob/master/LICENSE) license.

CameraView is licensed under the [MIT](https://github.com/natario1/CameraView/blob/master/LICENSE) license as well.
