import java.util.Properties
import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
val product = Properties().apply { rootProject.file("product.properties").reader(Charsets.UTF_8).use { load(it) } }
fun value(name: String) = product.getProperty(name) ?: error("Missing product property: " + name)
fun quoted(text: String) = "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"")
    .replace("\n", "\\n").replace("\r", "\\r") + "\""
fun environment(name: String) = Properties().apply {
    val file = rootProject.file("environments/$name.properties")
    if (file.exists()) file.reader(Charsets.UTF_8).use { load(it) }
}
val testEnvironment = environment("test")
// Missing production values stay empty/disabled for structural compilation; packaging is gated below.
val productionEnvironment = environment("production")
val environmentStrings = listOf("API_BASE_URL", "WECHAT_APP_ID", "FEEDBACK_URL", "PRIVACY_POLICY_URL",
    "TERMS_URL", "PRIVACY_VERSION", "ONBOARDING_VERSION", "SUPPORT_EMAIL", "OPERATOR_NAME",
    "APP_RECORD_NUMBER", "APP_MARKET_URL")
fun com.android.build.api.dsl.ApplicationBuildType.configureEnvironment(name: String, config: Properties) {
    buildConfigField("String", "ENVIRONMENT", quoted(name))
    environmentStrings.forEach { buildConfigField("String", it, quoted(config.getProperty(it, "").trim())) }
    listOf("SMS_ENABLED", "WECHAT_ENABLED", "ONBOARDING_ENABLED").forEach {
        buildConfigField("boolean", it, config.getProperty(it, "false").toBooleanStrict().toString())
    }
    buildConfigField("boolean", "DEMO_ENABLED", config.getProperty("DEMO_ENABLED", "true").toBooleanStrict().toString())
    buildConfigField("boolean", "TEST_TOOLS_ENABLED", if (name == "production") "false"
        else config.getProperty("TEST_TOOLS_ENABLED", "false").toBooleanStrict().toString())
}
require(value("APPLICATION_ID").matches(Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+")))

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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true; resValues = true }
    val signingFile = rootProject.file("keystore.properties")
    val signing = Properties().apply { if (signingFile.exists()) signingFile.reader().use { load(it) } }
    if (listOf("storeFile", "storePassword", "keyAlias", "keyPassword").all { !signing.getProperty(it).isNullOrBlank() }) {
        signingConfigs.create("productRelease") {
            storeFile = rootProject.file(signing.getProperty("storeFile"))
            storePassword = signing.getProperty("storePassword")
            keyAlias = signing.getProperty("keyAlias")
            keyPassword = signing.getProperty("keyPassword")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildTypes {
        debug { configureEnvironment("test", testEnvironment) }
        release {
            configureEnvironment("production", productionEnvironment)
            isMinifyEnabled = true; isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("productRelease")?.let { signingConfig = it }
        }
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
val verifyReleaseReady = tasks.register<Exec>("verifyReleaseReady") {
    group = "verification"
    description = "Validate production configuration, legal URL format and release signing before packaging."
    workingDir(rootProject.projectDir)
    val javaExecutable = File(System.getProperty("java.home"), "bin/java" +
        if (System.getProperty("os.name").startsWith("Windows")) ".exe" else "")
    commandLine(javaExecutable.absolutePath, "scripts/VerifyReleaseReady.java", rootProject.projectDir.absolutePath)
}
// Gate direct package/bundle/assemble invocations as well as their usual lifecycle tasks.
// Do not attach to compileReleaseKotlin: it must remain usable without production credentials.
tasks.configureEach {
    // Exclude resource/class bundle intermediate tasks used by compileReleaseKotlin.
    if (name in setOf("packageRelease", "packageReleaseBundle", "packageReleaseUniversalApk",
            "packageReleaseApk", "bundleRelease", "assembleRelease", "signReleaseBundle")) {
        dependsOn(verifyReleaseReady)
    }
}
dependencies {
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
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
