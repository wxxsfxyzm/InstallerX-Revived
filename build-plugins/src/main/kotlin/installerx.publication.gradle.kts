import com.android.build.api.dsl.LibraryExtension

plugins {
    `maven-publish`
}

val publicationExt = extensions.create<InstallerxPublicationExtension>("installerxPublication")
val publicationVersion = providers.gradleProperty("UI_COMPONENTS_VERSION").orElse("0.1.0-SNAPSHOT")
val projectUrl = "https://github.com/wxxsfxyzm/InstallerX-Revived"

plugins.withId("com.android.library") {
    extensions.configure<LibraryExtension> {
        publishing {
            singleVariant("release") {
                withSourcesJar()
            }
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.rosan.installer"
            artifactId = project.name
            version = publicationVersion.get()
            afterEvaluate {
                from(components["release"])
            }
            pom {
                name.set(project.name)
                description.set(publicationExt.description)
                url.set(projectUrl)
                licenses {
                    license {
                        name.set("GPL-3.0-only")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                    }
                }
                issueManagement {
                    system.set("GitHub")
                    url.set("$projectUrl/issues")
                }
                scm {
                    connection.set("scm:git:$projectUrl.git")
                    url.set(projectUrl)
                }
            }
        }
    }
}
