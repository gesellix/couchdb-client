import java.text.SimpleDateFormat
import java.util.*

buildscript {
  extra.apply {
    set("groovyVersion", "2.5.13")
    set("logbackVersion", "1.2.3")
    set("slf4jVersion", "1.7.30")
    set("spockVersion", "1.3-groovy-2.5")
  }
}

plugins {
  id("groovy")
  id("java-library")
  id("maven-publish")
  id("signing")
  id("com.github.ben-manes.versions") version "0.36.0"
  id("net.ossindex.audit") version "0.4.11"
  id("io.freefair.maven-central.validate-poms") version "5.3.0"
  id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.codehaus.groovy:groovy:${rootProject.extra["groovyVersion"]}")
  implementation("org.codehaus.groovy:groovy-json:${rootProject.extra["groovyVersion"]}")

  implementation("org.slf4j:slf4j-api:${rootProject.extra["slf4jVersion"]}")
  testRuntimeOnly("ch.qos.logback:logback-classic:${rootProject.extra["logbackVersion"]}")

  testImplementation("org.spockframework:spock-core:${rootProject.extra["spockVersion"]}")
  testImplementation("cglib:cglib-nodep:3.3.0")
  testImplementation("com.jayway.jsonpath:json-path:2.5.0")
  testImplementation("com.jayway.jsonpath:json-path-assert:2.5.0")
  testImplementation("org.testcontainers:spock:1.15.2")

  implementation("com.squareup.okhttp3:okhttp:4.9.1")
  implementation("com.squareup.moshi:moshi:1.11.0")
}

val dependencyVersions = listOf(
  "com.squareup.okio:okio:2.8.0",
  "net.java.dev.jna:jna:5.5.0",
  "org.jetbrains:annotations:17.0.0",
  "org.slf4j:slf4j-api:${extra["slf4jVersion"]}"
)

val dependencyVersionsByGroup = mapOf(
  "org.jetbrains.kotlin" to "1.4.10",
  "org.codehaus.groovy" to extra["groovyVersion"] as String
)

configurations.all {
  resolutionStrategy {
    failOnVersionConflict()

    force(dependencyVersions)
    eachDependency {
      val forcedVersion = dependencyVersionsByGroup[requested.group]
      if (forcedVersion != null) {
        useVersion(forcedVersion)
      }
    }
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

val javadocJar by tasks.registering(Jar::class) {
  dependsOn("classes")
  archiveClassifier.set("javadoc")
  from(tasks.javadoc)
}

val sourcesJar by tasks.registering(Jar::class) {
  dependsOn("classes")
  archiveClassifier.set("sources")
  from(sourceSets.main.get().allSource)
}

artifacts {
  add("archives", sourcesJar.get())
  add("archives", javadocJar.get())
}

fun findProperty(s: String) = project.findProperty(s) as String?

val isSnapshot = project.version == "unspecified"
val artifactVersion = if (!isSnapshot) project.version as String else SimpleDateFormat("yyyy-MM-dd\'T\'HH-mm-ss").format(Date())!!
val publicationName = "couchdbClient"
publishing {
  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/${property("github.package-registry.owner")}/${property("github.package-registry.repository")}")
      credentials {
        username = System.getenv("GITHUB_ACTOR") ?: findProperty("github.package-registry.username")
        password = System.getenv("GITHUB_TOKEN") ?: findProperty("github.package-registry.password")
      }
    }
  }
  publications {
    register(publicationName, MavenPublication::class) {
      pom {
        name.set("couchdb-client")
        description.set("A CouchDB client written in Groovy")
        url.set("https://github.com/gesellix/couchdb-client")
        licenses {
          license {
            name.set("MIT")
            url.set("https://opensource.org/licenses/MIT")
          }
        }
        developers {
          developer {
            id.set("gesellix")
            name.set("Tobias Gesellchen")
            email.set("tobias@gesellix.de")
          }
        }
        scm {
          connection.set("scm:git:github.com/gesellix/couchdb-client.git")
          developerConnection.set("scm:git:ssh://github.com/gesellix/couchdb-client.git")
          url.set("https://github.com/gesellix/couchdb-client")
        }
      }
      artifactId = "couchdb-client"
      version = artifactVersion
      from(components["java"])
      artifact(sourcesJar.get())
      artifact(javadocJar.get())
    }
  }
}

signing {
  val signingKey: String? by project
  val signingPassword: String? by project
  useInMemoryPgpKeys(signingKey, signingPassword)
  sign(publishing.publications[publicationName])
}

nexusPublishing {
  repositories {
    if (!isSnapshot) {
      sonatype {
        // 'sonatype' is pre-configured for Sonatype Nexus (OSSRH) which is used for The Central Repository
        stagingProfileId.set(System.getenv("SONATYPE_STAGING_PROFILE_ID") ?: findProperty("sonatype.staging.profile.id")) //can reduce execution time by even 10 seconds
        username.set(System.getenv("SONATYPE_USERNAME") ?: findProperty("sonatype.username"))
        password.set(System.getenv("SONATYPE_PASSWORD") ?: findProperty("sonatype.password"))
      }
    }
  }
}

//    pkg {
//        repo = "couchdb"
//        name = "couchdb-client"
//        desc = "A CouchDB client written in Groovy"
//        licenses = ["Apache-2.0"]
//        labels = ["couchdb", "client"]
//        version = [
//            name: artifactVersion
//        ]
//        vcsUrl = "https://github.com/gesellix/couchdb-client"
//    }
