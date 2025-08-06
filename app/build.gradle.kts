import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kapt)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
android {
    namespace = "com.example.reservely"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.reservely"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "CLOUDINARY_CLOUD_NAME",
            "\"${localProperties.getProperty("cloudinary_cloud_name")}\""
        )
        buildConfigField(
            "String",
            "CLOUDINARY_UPLOAD_PRESET",
            "\"${localProperties.getProperty("cloudinary_upload_preset")}\""
        )

        buildConfigField(
            "String",
            "GOOGLE_API_KEY",
            "\"${localProperties.getProperty("google_api_key")}\""
        )

        buildConfigField(
            "String",
            "CHAT_API_KEY",
            "\"${localProperties.getProperty("chat_api_key")}\""
        )
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.play.services.auth)
    implementation(libs.firebase.functions.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.material.icons.extended.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose) // or latest version

    implementation(libs.navigation.compose)
    implementation(libs.androidx.ui.text.google.fonts)

    implementation(libs.androidx.foundation)

    implementation(libs.coil.compose)

    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.storage.ktx)

    implementation(libs.places)

    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.logging.interceptor)
    implementation("com.google.accompanist:accompanist-swiperefresh:0.33.2-alpha")

    implementation(libs.firebase.messaging)
    implementation (libs.androidx.security.crypto)
    implementation (libs.stream.chat.android.compose)
    implementation (libs.stream.chat.android.client)
    implementation (libs.stream.chat.android.offline)
    implementation (libs.stream.chat.android.state)

    implementation(libs.hilt)
    kapt(libs.hiltCompiler)

    implementation(libs.hiltNavigationCompose)

}

