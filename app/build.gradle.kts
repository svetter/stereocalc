import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.jetbrains.kotlin.android)
	id("com.google.android.gms.oss-licenses-plugin")
}

android {
	namespace = "com.gmail.simetist.stereophoniccalculator"
	compileSdk = 36
	
	defaultConfig {
		applicationId = "com.gmail.simetist.stereophoniccalculator"
		minSdk = 24
		targetSdk = 35
		versionCode = 4
		versionName = "1.0.1"
		
		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}
	
	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
			signingConfig = signingConfigs.getByName("debug")
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
	buildFeatures {
		mlModelBinding = true
	}
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.fromTarget("1.8")
	}
}

dependencies {
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.appcompat)
	implementation(libs.material)
	implementation(libs.androidx.activity)
	implementation(libs.androidx.constraintlayout)
	implementation(libs.play.services.oss.licenses)
	testImplementation(libs.junit)
	androidTestImplementation(libs.androidx.junit)
	androidTestImplementation(libs.androidx.espresso.core)
}