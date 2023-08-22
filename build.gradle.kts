import java.text.SimpleDateFormat
import java.util.*

plugins {
  id("groovy")
  id("java-library")
  id("maven-publish")
  id("signing")
  id("com.github.ben-manes.versions") version "0.47.0"
  id("net.ossindex.audit") version "0.4.11"
  id("io.freefair.maven-central.validate-poms") version "8.1.0"
  id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

repositories {
  mavenCentral()
}

val groovyVersion = "4.0.13"
val kotlinVersion = "1.6.20"

dependencies {
  constraints {
    listOf(
      "com.squareup.okio:okio",
      "com.squareup.okio:okio-jvm"
    ).onEach {
      implementation(it) {
        version {
          strictly("[3,4)")
          prefer("3.3.0")
        }
      }
    }
  }
  implementation("org.apache.groovy:groovy:${groovyVersion}")
  implementation("org.apache.groovy:groovy-json:${groovyVersion}")

  implementation("org.slf4j:slf4j-api:1.7.36")
  testRuntimeOnly("ch.qos.logback:logback-classic:1.2.11")

  testImplementation("org.spockframework:spock-core:2.3-groovy-4.0")
  testImplementation("cglib:cglib-nodep:3.3.0")
  testImplementation("com.jayway.jsonpath:json-path:2.8.0")
  testImplementation("com.jayway.jsonpath:json-path-assert:2.8.0")
  testImplementation("org.testcontainers:spock:1.19.0")

  implementation("com.squareup.okhttp3:okhttp:4.11.0")
  implementation("com.squareup.moshi:moshi:1.15.0")
}

val dependencyVersions = listOf(
  "com.squareup.okio:okio:3.3.0",
  "net.java.dev.jna:jna:5.13.0",
  "org.jetbrains:annotations:24.0.1",
  "org.slf4j:slf4j-api:1.7.36",
  "org.spockframework:spock-core:2.3-groovy-4.0",
  "org.ow2.asm:asm:9.5",
)

val dependencyVersionsByGroup = mapOf(
  "org.jetbrains.kotlin" to kotlinVersion,
  "org.apache.groovy" to groovyVersion,
  "org.codehaus.groovy" to groovyVersion,
)

configurations.all {
  resolutionStrategy {
    dependencySubstitution {
      all {
        requested.let {
          if (it is ModuleComponentSelector && it.group == "org.codehaus.groovy") {
            logger.lifecycle("substituting $it to groupId 'org.apache.groovy'")
            useTarget(
              "org.apache.groovy:${it.module}:${it.version}",
              "Changed Maven coordinates since Groovy 4"
            )
          }
        }
      }
    }

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

tasks.withType<Test> {
  useJUnitPlatform()
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
  setRequired( { gradle.taskGraph.hasTask("uploadArchives") })
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

tasks.wrapper {
// https://gradle.org/releases/
  gradleVersion = "8.1.1"
  distributionType = Wrapper.DistributionType.BIN
// https://gradle.org/release-checksums/
  distributionSha256Sum = "e111cb9948407e26351227dabce49822fb88c37ee72f1d1582a69c68af2e702f"
}
