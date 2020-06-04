apply(plugin = "com.android.application")
apply(plugin = "kotlin-android")
plugins {
    id("com.android.application")
    kotlin("android")
}

repositories {
    google()
    jcenter()
    maven("https://oss.jfrog.org/libs-release")
}

android {
    compileSdkVersion(29)
    buildToolsVersion("29.0.3")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // Why isn't a BuildConfig file generated?
        all {
            buildConfigField("String", "OMDB_KEY", "\"${file("omdb.key").readText()}\"")
        }
    }
}
tasks.withType(Test::class.java) {
    useJUnitPlatform()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("com.squareup.retrofit2:retrofit:2.7.1")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.7.1")
    implementation("com.squareup.retrofit2:converter-jackson:2.7.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.10")
    implementation("io.reactivex.rxjava2:rxandroid:2.0.0")
    // implementation("io.reactivex.rxjava2:rxkotlin:2.2.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
}