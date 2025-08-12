plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("org.jetbrains.kotlinx.rpc.plugin") version "0.9.1"
}

repositories {
    mavenCentral()
}

val ktorVersion = "3.2.0"
val junitVersion = "5.13.4"
val rpcVersion = "0.9.1"
val coroutinesTest = "1.10.2"

dependencies {
    // Ktor basics for tests & sample server endpoints
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets-jvm:$ktorVersion")

    // Kotlinx RPC / kRPC + Ktor integration
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-client:$rpcVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-server:$rpcVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-serialization-json:$rpcVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-ktor-client:$rpcVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-ktor-server:$rpcVersion")

    // Testing
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesTest")
}

tasks.test {
    useJUnitPlatform()
    // Ktor test host can be chatty; keep logs tidy in CI
    systemProperty("logback.configurationFile", "none")

    // Show standard out/err from tests
    testLogging {
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        events("passed", "skipped", "failed")
    }
}
