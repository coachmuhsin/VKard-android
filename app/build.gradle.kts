import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

val versionProperties = Properties()
val versionPropertiesFile = rootProject.file("version.properties")
if (versionPropertiesFile.exists()) {
    versionPropertiesFile.inputStream().use { versionProperties.load(it) }
}

val extVersionCode = if (project.hasProperty("versionCode")) {
    project.property("versionCode").toString().toInt()
} else if (versionProperties.containsKey("versionCode")) {
    versionProperties.getProperty("versionCode").toString().toInt()
} else {
    1
}

val extVersionName = if (project.hasProperty("versionName")) {
    project.property("versionName").toString()
} else if (versionProperties.containsKey("versionName")) {
    versionProperties.getProperty("versionName").toString()
} else {
    "Release 1"
}

val supabaseUrl = localProperties.getProperty("SUPABASE_URL") ?: System.getenv("SUPABASE_URL") ?: ""
val supabaseKey = localProperties.getProperty("SUPABASE_KEY") ?: System.getenv("SUPABASE_KEY") ?: ""

android {
    namespace = "com.vkard.pro"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vkard.pro"
        minSdk = 26
        targetSdk = 34
        versionCode = extVersionCode
        versionName = extVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = localProperties.getProperty("RELEASE_STORE_FILE")
            if (!storeFilePath.isNullOrEmpty()) {
                storeFile = file(storeFilePath)
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
            } else {
                val debugConfig = getByName("debug")
                storeFile = debugConfig.storeFile
                storePassword = debugConfig.storePassword
                keyAlias = debugConfig.keyAlias
                keyPassword = debugConfig.keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX & Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Security (EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Supabase Kotlin SDK
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.2.3")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.2.3")

    // Ktor HTTP Client
    implementation("io.ktor:ktor-client-android:2.3.11")
    implementation("io.ktor:ktor-client-core:2.3.11")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
 
    // OkHttp Client (Audited OTA updates requirement)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
 
    // AndroidX Browser Custom Tabs
    implementation("androidx.browser:browser:1.8.0")

    // Image loading (Coil)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // QR Code generation
    implementation("com.google.zxing:core:3.5.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
