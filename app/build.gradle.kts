import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.chatease"
    compileSdk = 34

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources.excludes.add("META-INF/DEPENDENCIES")
        resources.excludes.add("META-INF/INDEX.LIST")
    }

    defaultConfig {
        applicationId = "com.example.chatease"
        minSdk = 27 // Android 8.1
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        ndkVersion = "25.2.9519653"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    applicationVariants.all {
       outputs.all {
           val outputImpl = this as BaseVariantOutputImpl

           outputImpl.outputFileName = "ChatEase"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.database)
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Scalable Size Unit (Support for different screen sizes)
    implementation(libs.intuit.sdp)
    implementation(libs.intuit.ssp)

    // Ramen's Rounded ImageView
    implementation(libs.makeramen.roundedimageview)

    // Picasso (An easy image loading library)
    implementation("com.squareup.picasso:picasso:2.71828")

    // Glide (Advanced Image Loading Library)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Native version of uCrop (Image Cropping Library for Android) for enhanced image quality
    implementation("com.github.yalantis:ucrop:2.2.10")

    // Libraries of LifeCycle Observer
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1") // For Latest versions
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0") // For older versions

    // For handling FCM OAuth Key Registration and Regeneration
    implementation("com.google.auth:google-auth-library-oauth2-http:1.37.1")

    // For handling HTTP requests
    implementation("com.squareup.okhttp3:okhttp:5.1.0")

    // Material UI Components
    implementation("com.google.android.material:material:1.12.0")

    //For Implementing Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // For Implementing EncryptedSharedPreference for storing FCM Token
    implementation("androidx.security:security-crypto:1.1.0-beta01")
}
