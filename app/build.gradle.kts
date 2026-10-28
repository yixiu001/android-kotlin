import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
val product = Properties().apply { rootProject.file("product.properties").reader(Charsets.UTF_8).use { load(it) } }
fun value(name: String) = product.getProperty(name) ?: error("Missing product property: " + name)
fun quoted(text: String) = "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
require(value("APPLICATION_ID").matches(Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+")))
require(value("API_BASE_URL").startsWith("https://"))

android {
    namespace = "cn.yixiu.starter"
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = value("APPLICATION_ID")
        minSdk = 26
        targetSdk = 37
        versionCode = value("VERSION_CODE").toInt()
        versionName = value("VERSION_NAME")
        resValue("string", "app_name", value("APP_NAME"))
        buildConfigField("String", "API_BASE_URL", quoted(value("API_BASE_URL")))
        buildConfigField("String", "WECHAT_APP_ID", quoted(value("WECHAT_APP_ID")))
        buildConfigField("boolean", "SMS_ENABLED", value("SMS_ENABLED"))
        buildConfigField("boolean", "WECHAT_ENABLED", value("WECHAT_ENABLED"))
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true; resValues = true }
    val signingFile = rootProject.file("keystore.properties")
    if (signingFile.exists()) {
        val signing = Properties().apply { signingFile.reader().use { load(it) } }
        signingConfigs.create("productRelease") {
            storeFile = rootProject.file(signing.getProperty("storeFile"))
            storePassword = signing.getProperty("storePassword")
            keyAlias = signing.getProperty("keyAlias")
            keyPassword = signing.getProperty("keyPassword")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildTypes {
        debug { buildConfigField("boolean", "TEST_TOOLS_ENABLED", product.getProperty("TEST_TOOLS_ENABLED", "false").toBooleanStrict().toString()) }
        release {
            buildConfigField("boolean", "TEST_TOOLS_ENABLED", "false")
            isMinifyEnabled = true; isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("productRelease")?.let { signingConfig = it }
        }
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.tencent.mm.opensdk:wechat-sdk-android:6.8.40")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
