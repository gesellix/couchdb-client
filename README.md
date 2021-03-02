# A Groovy CouchDB Client

[![Publish](https://github.com/gesellix/couchdb-client/actions/workflows/cd.yml/badge.svg)](https://github.com/gesellix/couchdb-client/actions/workflows/cd.yml)
[![Maven Central](https://github.com/gesellix/couchdb-client/actions/workflows/release.yml/badge.svg)](https://github.com/gesellix/couchdb-client/actions/workflows/release.yml)
[Latest version on Maven Central](https://search.maven.org/artifact/de.gesellix/couchdb-client)

Yeah, this is another very thin layer on top of [CouchDB HTTP API](https://docs.couchdb.org/en/2.3.1/intro/api.html).

There's nothing special with it, and it only exists because other solutions are often too over engineered.

You won't find wonderful abstractions, only the more or less plain adaption to the underlying HTTP client.

### Usage (Gradle)

````kotlin
repositories {
  mavenCentral()
}
dependencies {
  implementation("de.gesellix:couchdb-client:2021-03-02T23-21-53")
}
````

The integration test at `de.gesellix.couchdb.CouchDBClientIntegrationSpec` shows all supported use cases.

## Release Workflow

There are multiple GitHub Action Workflows for the different steps in the package's lifecycle:

- CI: Builds and checks incoming changes on a pull request
  - triggered on every push to a non-default branch
- CD: Publishes the Gradle artifacts to GitHub Package Registry
  - triggered only on pushes to the default branch
- Release: Publishes Gradle artifacts to Sonatype and releases them to Maven Central
  - triggered on a published GitHub release using the underlying tag as artifact version, e.g. via `git tag -m "$MESSAGE" v$(date +"%Y-%m-%dT%H-%M-%S")`

### Contributing

If you miss some feature or find an issue, please contact me via [issue tracker](https://github.com/gesellix/couchdb-client/issues).
If you'd like to contribute code, you're very welcome to submit a [pull request](https://github.com/gesellix/couchdb-client/pulls)!

You may also contact me via Twitter [@gesellix](https://twitter.com/gesellix).
