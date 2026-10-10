// Desktop-JVM harness that runs the *real* Kotlin audio pipeline (DfnRuntime + ONNX Runtime + JNI + native C++)
// against the real DeepFilterNet3 models and a real noisy recording. It reuses the production sources unchanged
// (the audio package has no android.* imports on purpose), so JNI marshalling, chunking and alignment bugs show up
// here instead of on a phone.
//
//   cmake -S app/src/main/cpp -B /tmp/aura-host -DCMAKE_BUILD_TYPE=Release && cmake --build /tmp/aura-host
//   ./gradlew :hosttest:test -Pauramic.hostlib=/tmp/aura-host -Pauramic.sample=/path/noisy.wav -Pauramic.clean=/path/clean.wav
// Without those properties the tests are skipped.
plugins { alias(libs.plugins.kotlin.jvm) }

kotlin { jvmToolchain(21) }

sourceSets {
    main {
        kotlin.setSrcDirs(listOf("../app/src/main/java"))
        kotlin.include(
            "com/kaafisir/auramic/audio/AudioLog.kt", "com/kaafisir/auramic/audio/DfnRuntime.kt",
            "com/kaafisir/auramic/audio/NativeDfn.kt", "com/kaafisir/auramic/audio/NativePolish.kt",
            "com/kaafisir/auramic/audio/NativeDenoiser.kt", "com/kaafisir/auramic/audio/Resampler.kt",
            "com/kaafisir/auramic/audio/SilenceCutter.kt", "com/kaafisir/auramic/audio/EnhancementPipeline.kt",
            "com/kaafisir/auramic/domain/model/Models.kt",
        )
    }
}

dependencies {
    implementation(libs.onnxruntime.jvm)
    implementation("javax.inject:javax.inject:1")
    testImplementation(libs.junit)
}

tasks.test {
    val lib = providers.gradleProperty("auramic.hostlib").orNull
    val sample = providers.gradleProperty("auramic.sample").orNull
    val clean = providers.gradleProperty("auramic.clean").orNull
    if (lib != null) systemProperty("java.library.path", lib)
    if (sample != null) systemProperty("auramic.sample", sample)
    if (clean != null) systemProperty("auramic.clean", clean)
    providers.gradleProperty("auramic.reference").orNull?.let { systemProperty("auramic.reference", it) }
    systemProperty("auramic.models", file("../app/src/main/assets/dfn3").absolutePath)
    maxHeapSize = "2g"
    testLogging { showStandardStreams = true }
}
