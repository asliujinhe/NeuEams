import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish") version "0.28.0"

}

group = "org.jetos"
version = "1.0.3"



repositories {
    mavenCentral()
    google()
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(kotlin("stdlib"))
    //coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.mozilla:rhino:1.7.15")
    //gson
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.jsoup:jsoup:1.15.3")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
    coordinates(group as String, "neu-eams-util", version as String)

    pom {
        name.set("NEU EAMS Util")
        description.set("Utils for NEU EAMS, for parsing courses of NEU EAMS")
        inceptionYear.set("2024")
        url.set("https://jetos.org")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("asliujinhe")
                name.set("Jim Liu")
                email.set("asliujinhe@gmail.com")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/asliujinhe/NeuEams.git")
            url.set("https://jetos.org")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}