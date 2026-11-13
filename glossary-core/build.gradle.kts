plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

kotlin {
    jvmToolchain(17)
    explicitApi()
}

java {
    withSourcesJar()
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            from(components["java"])
            groupId = "io.github.mlkitglossary"
            artifactId = "glossary-core"
            version = "0.1.1"
            pom {
                name.set("glossary-core")
                description.set("Pure-Kotlin glossary and placeholder protection core for mlkit-glossary-translate")
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }
}
