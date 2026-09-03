plugins { id("com.android.application") }

android {
    namespace = "gr.vpapaion.motogauge"
    compileSdk = 35

    defaultConfig {
        applicationId = "gr.vpapaion.motogauge"
        minSdk = 23
        targetSdk = 35
        versionCode = 5
        versionName = "1.2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
