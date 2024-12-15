plugins {
    alias(libs.plugins.android.application)
    id ("org.jetbrains.kotlin.android")
//    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compilerKsp)
}

android {

    namespace = "com.example.regontition_app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.regontition_app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
//    val room_version = "2.6.1"
//
//    ksp("androidx.room:room-compiler:$room_version")
//    implementation(libs.androidx.room.common)
//    implementation(libs.androidx.room.ktx)
//    annotationProcessor("androidx.room:room-compiler:$room_version")
//    implementation("androidx.room:room-runtime:$room_version")

    implementation(libs.androidx.room)
    ksp(libs.androidx.room.ksp)


    implementation (libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}