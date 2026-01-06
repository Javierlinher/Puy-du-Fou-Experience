import java.util.Properties

plugins {
    // Plugin principal de Android (app)
    alias(libs.plugins.android.application)

    // Plugin de Kotlin para Android
    alias(libs.plugins.kotlin.android)

    // KSP: necesario para que Room genere código automáticamente al compilar (DAOs, Database, etc.)
    alias(libs.plugins.ksp)
}

// ✅ Cargar claves desde local.properties (porque project.findProperty NO siempre lo lee)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

// Clave para Google Maps SDK (SDK nativo de Maps)
val mapsApiKey: String = localProps.getProperty("MAPS_API_KEY") ?: ""

// Clave para Google Routes API (HTTP)
val routesApiKey: String = localProps.getProperty("ROUTES_API_KEY") ?: ""

android {
    namespace = "com.example.puydufouexperience"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.puydufouexperience"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ✅ Inyecta la key de MAPS en el Manifest como ${MAPS_API_KEY}
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey

        // ✅ Expone la key de ROUTES en BuildConfig
        // OJO: las comillas dobles dentro son OBLIGATORIAS
        buildConfigField(
            "String",
            "ROUTES_API_KEY",
            "\"$routesApiKey\""
        )
    }

    // ViewBinding + BuildConfig
    buildFeatures {
        viewBinding = true

        // ✅ NECESARIO para que exista BuildConfig
        buildConfig = true
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
}

dependencies {

    // AndroidX básicos
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Navigation Component (KTX)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // AppCompat (idiomas / recursos)
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.appcompat:appcompat-resources:1.7.1")

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Google Maps + Location
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
}
