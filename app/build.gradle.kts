plugins { id("com.android.application") }

android {
    namespace = "gr.vpapaion.motogauge"
    compileSdk = 35

    defaultConfig {
        applicationId = "gr.vpapaion.motogauge"
        minSdk = 23
        targetSdk = 35
        versionCode = 7
        versionName = "1.2.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
