Running `./gradlew clean assembleDebug --rerun-tasks -i > log.txt` to get the log.

A copy of [log.txt](error-log.txt) is placed in the root folder:

- Line 309-323: transforming `kotlin-stdlib-jdk8-1.7.22.jar` ` kotlin-stdlib-jdk7-1.7.22.jar` `kotlin-stdlib-1.7.22.jar` `annotations-13.0.jar`: The Gradle transform action happened before `:app:clean`.
- Line 228887-end: Failed to transform `kotlin-stdlib-jdk8-1.7.22.jar` ` kotlin-stdlib-jdk7-1.7.22.jar` `kotlin-stdlib-1.7.22.jar` `annotations-13.0.jar`: By the time the aggregation file is not existed so it throws exception.