[![Build Status](https://img.shields.io/github/actions/workflow/status/gesellix/couchdb-client/cd.yml?branch=main&style=for-the-badge)](https://github.com/gesellix/couchdb-client/actions)
[![Maven Central](https://img.shields.io/maven-central/v/de.gesellix/couchdb-client.svg?style=for-the-badge&maxAge=86400)](https://search.maven.org/search?q=g:de.gesellix%20AND%20a:couchdb-client)

# A Groovy CouchDB Client

Yeah, this is another very thin layer on top of [CouchDB HTTP API](https://docs.couchdb.org/en/2.3.1/intro/api.html).

There's nothing special with it, and it only exists because other solutions are often too over engineered.

You won't find wonderful abstractions, only the more or less plain adaption to the underlying HTTP client.

### Usage (Gradle)

````kotlin
repositories {
  mavenCentral()
}
dependencies {
  implementation("de.gesellix:couchdb-client:<version>")
}
````

The integration test at `de.gesellix.couchdb.CouchDbClientIntegrationSpec` shows all supported use cases.

### Contributing

If you miss some feature or find an issue, please contact me via [issue tracker](https://github.com/gesellix/couchdb-client/issues).
If you'd like to contribute code, you're very welcome to submit a [pull request](https://github.com/gesellix/couchdb-client/pulls)!

You may also contact me via Twitter [@gesellix](https://twitter.com/gesellix).

## Publishing/Release Workflow

See RELEASE.md

## License

MIT License

Copyright 2015-2021 [Tobias Gesellchen](https://www.gesellix.net/) ([@gesellix](https://twitter.com/gesellix))

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
