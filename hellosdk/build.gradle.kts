plugins {
    id("com.android.library") version "8.2.1"
    id("org.jetbrains.kotlin.android") version "1.9.0"
    id("maven-publish")
    id("signing")
    id("com.gradleup.nmcp").version("0.0.8")
}

android {
    namespace = "com.msg91.hellosdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    // Activity Result API for modern file upload handling
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

group = "com.msg91.lib"
version = "1.0.0"
val artifactName = "hellosdk"
val artifactDescription = "Hello SDK for Android by MSG91"
val artifactUrl = "https://github.com/Walkover-Web-Solution/MSG91-hello-kotlin-sdk"

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = group.toString()
                artifactId = artifactName
                version = version

                pom {
                    name.set(artifactName)
                    description.set(artifactDescription)
                    url.set(artifactUrl)

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("vikas")
                            name.set("Vikas Dubey")
                            email.set("vikas@walkover.in")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/Walkover-Web-Solution/MSG91-hello-kotlin-sdk.git")
                        developerConnection.set("scm:git:ssh://github.com/Walkover-Web-Solution/MSG91-hello-kotlin-sdk.git")
                        url.set(artifactUrl)
                    }
                }
            }
        }

        repositories {
            maven {
                name = "OSSRH"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = project.findProperty("SONATYPE_USER_NAME") as String?
                    password = project.findProperty("SONATYPE_PASSWORD") as String?
                }
            }
        }
    }

    signing {
        sign(publishing.publications["release"])
    }

    nmcp {
        publishAllPublications {
            username = findProperty("SONATYPE_USER_NAME") as String? ?: ""
            password = findProperty("SONATYPE_PASSWORD") as String? ?: ""
            publicationType = "USER_MANAGED"
        }
    }
}