plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

val pluginName = "StepCounterPlugin"
val pluginPackageName = "com.carleinarhellenes.stepengine"

android {
    namespace = pluginPackageName
    compileSdk = 35

    defaultConfig {
        minSdk = 26  // Android 8.0 — covers all modern devices
        setProperty("archivesBaseName", pluginName)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // compileOnly: Godot provides the runtime; this is just for compilation.
    // Update to match your Godot version if needed.
    // Available versions: https://central.sonatype.com/artifact/org.godotengine/godot/versions
    compileOnly("org.godotengine:godot:4.4.0.stable")
}
