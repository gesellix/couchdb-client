# A Groovy CouchDB Client 

[![Build Status](https://travis-ci.org/gesellix/couchdb-client.svg?branch=master)](https://travis-ci.org/gesellix/couchdb-client)
[ ![Download](https://api.bintray.com/packages/gesellix/couchdb/couchdb-client/images/download.svg) ](https://bintray.com/gesellix/couchdb/couchdb-client/_latestVersion)

Yeah, this is another very thin layer on top of [CouchDB HTTP API](http://docs.couchdb.org/en/1.6.1/intro/api.html).

There's nothing special with it, and it only exists because other solutions are often too over engineered.

You won't find wonderful abstractions, only the more or less plain adaption to the underlying HTTP client.

### Usage (Gradle)

````
repositories {
    maven { url 'http://dl.bintray.com/gesellix/couchdb' }
    mavenCentral()
}
dependencies {
    compile 'de.gesellix:couchdb-client:2016-10-30_481362'
}
````

The integration test at `de.gesellix.couchdb.CouchDBClientIntegrationSpec` shows all supported use cases.

### Contributing

If you miss some feature or find an issue, please contact me via [issue tracker](https://github.com/gesellix/couchdb-client/issues).
If you'd like to contribute code, you're very welcome to submit a [pull request](https://github.com/gesellix/couchdb-client/pulls)! 

You may also contact me via Twitter [@gesellix](https://twitter.com/gesellix).
