plugins {
    id("com.android.application") version "9.4.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    id("com.google.devtools.ksp") version "2.3.10"
}

android {
    namespace = "com.scazzumvivendi.appanettone"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.scazzumvivendi.appanettone"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "0.0.1"
    }

    buildFeatures { compose = true }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation("com.android.billingclient:billing:8.3.0")
    constraints {
        implementation("androidx.fragment:fragment:1.3.6") {
            because("Play Billing brings in an older Fragment version incompatible with Activity Result APIs")
        }
    }
    implementation(platform("androidx.compose:compose-bom:2026.02.01"))
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.room:room-runtime:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    testImplementation("junit:junit:4.13.2")
}
