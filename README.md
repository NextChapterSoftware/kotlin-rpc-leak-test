# kRPC/Ktor Memory Leak Tests (Gradle Kotlin)

Minimal project to reproduce and test for server-side memory leaks in:

- kRPC over Ktor
- Plain Ktor HTTP
- Ktor WebSockets

## Prereqs

* JDK 17+
* (Option A) **Gradle installed**: run `gradle wrapper --gradle-version 9.0.0` once to generate `gradle-wrapper.jar`.
* (Option B) If you already have the wrapper jar, just run `./gradlew test`.

> This zip intentionally omits the binary `gradle/wrapper/gradle-wrapper.jar`. If you don't have Gradle installed, you can either add that jar from another project or install Gradle and run the wrapper task above.

## Run tests

```bash
./gradlew test
```
