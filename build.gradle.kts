plugins {
    kotlin("jvm")
    `maven-publish`
    signing
}

group = "org.jetos"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    //coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.mozilla:rhino:1.7.15")
    implementation("com.alibaba:fastjson:2.0.50")

    implementation("org.jsoup:jsoup:1.15.3")


}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("Neu Eams Utils")
                description.set("Utils for NEU EAMS, for parsing courses of NEU EAMS")
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
                    connection.set("scm:git:git://example.com/project.git")
                    developerConnection.set("scm:git:ssh://example.com:project.git")
                    url.set("https://example.com/project")
                }
            }
        }
    }

    repositories {
        maven {
            val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

            credentials {
                username = project.findProperty("ossrhUsername") as String? ?: ""
                password = project.findProperty("ossrhPassword") as String? ?: ""
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}
