plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

// --- Versioning derived from git ---------------------------------------------
// Git tags are the single source of truth. versionName/versionCode are computed
// at build time so no version-bump commit is ever needed — see CLAUDE.md.
fun git(vararg args: String): String? = try {
    val out = providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }
    if (out.result.get().exitValue == 0)
        out.standardOutput.asText.get().trim().ifEmpty { null }
    else null
} catch (_: Exception) { null }   // git missing / not a repo

// Latest "v1.2.3" tag -> Triple(1,2,3); falls back to 0.0.0 when no tags exist.
fun latestSemverTag(): Triple<Int, Int, Int> {
    val tag = git("describe", "--tags", "--abbrev=0", "--match", "v[0-9]*")
        ?.removePrefix("v") ?: "0.0.0"
    val p = tag.split(".").mapNotNull { it.toIntOrNull() }
    return Triple(p.getOrElse(0) { 0 }, p.getOrElse(1) { 0 }, p.getOrElse(2) { 0 })
}

// Monotonic build number = total commit count (always-increasing positive int).
fun gitCommitCount(): Int = git("rev-list", "--count", "HEAD")?.toIntOrNull() ?: 1

// VERSION_BRANCH/VERSION_SHA let CI inject the true branch + commit. On a
// pull_request the reserved GITHUB_REF_NAME/GITHUB_SHA hold the merge ref
// ("<pr>/merge") and the ephemeral merge commit, and GitHub forbids overriding
// the GITHUB_* vars — so the workflow sets these unreserved names instead.
fun currentBranch(): String =
    System.getenv("VERSION_BRANCH")?.takeIf { it.isNotBlank() }
        ?: System.getenv("GITHUB_REF_NAME")
        ?: git("rev-parse", "--abbrev-ref", "HEAD") ?: "local"

fun shortSha(): String =
    (System.getenv("VERSION_SHA")?.takeIf { it.isNotBlank() }
        ?: System.getenv("GITHUB_SHA"))?.take(7)
        ?: git("rev-parse", "--short=7", "HEAD") ?: "nogit"

// True when HEAD sits exactly on a release tag (a clean release build).
fun isTaggedRelease(): Boolean =
    git("describe", "--tags", "--exact-match", "--match", "v[0-9]*") != null

// Clean X.Y.Z on main/tagged builds; X.Y.Z-<branch>.<sha> on branch builds.
fun computeVersionName(): String {
    val (maj, min, pat) = latestSemverTag()
    val base = "$maj.$min.$pat"
    val branch = currentBranch()
    return if (isTaggedRelease() || branch == "main" || branch == "HEAD") base
    else {
        val safe = branch.replace(Regex("[^A-Za-z0-9]+"), "-").trim('-').lowercase()
        "$base-$safe.${shortSha()}"
    }
}

// Exposes the computed versionName to CI so the APK filename matches the
// versionName baked into the APK exactly. Usage: `./gradlew -q :app:printVersionName`
tasks.register("printVersionName") {
    doLast { println(computeVersionName()) }
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.room.runtime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.activity.compose)
            implementation(project.dependencies.platform(libs.compose.bom))
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.graphics)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons)
            implementation(libs.navigation.compose)
            implementation(libs.sqlite.bundled)
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.datastore.preferences)
        }
    }
}

android {
    namespace = "com.mountaincrab.logrhythm"
    compileSdk = 35

    val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
    if (keystorePath != null) {
        signingConfigs {
            getByName("debug") {
                storeFile = file(keystorePath)
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    defaultConfig {
        applicationId = "com.mountaincrab.logrhythm"
        minSdk = 26
        targetSdk = 35
        versionCode = gitCommitCount()
        versionName = computeVersionName()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    sourceSets {
        getByName("debug") {
            assets.srcDir("$projectDir/schemas")
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("testImplementation", libs.room.testing)
    add("testImplementation", libs.androidx.test.runner)
    add("testImplementation", libs.androidx.test.ext.junit)
    add("testImplementation", libs.robolectric)
    add("debugImplementation", libs.compose.ui.tooling)
    add("androidMainImplementation", platform(libs.firebase.bom))
    add("androidMainImplementation", libs.firebase.auth)
    add("androidMainImplementation", libs.firebase.firestore)
    add("androidMainImplementation", libs.play.services.auth)
    add("androidMainImplementation", libs.work.runtime.ktx)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
