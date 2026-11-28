plugins { kotlin("jvm") version "2.2.10" }
kotlin {
    jvmToolchain(17)
    sourceSets {
        main {
            kotlin.srcDir("../app/src/main/java/com/example/data")
            kotlin.include("IndependentStageClient.kt")
        }
        test {
            kotlin.srcDir("../app/src/test/java/com/example/data")
            kotlin.include("IndependentStageClientTest.kt", "IndependentStageHttpTest.kt")
        }
    }
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    testImplementation("junit:junit:4.13.2")
}
tasks.test { useJUnit(); testLogging { events("passed","failed","skipped") } }
