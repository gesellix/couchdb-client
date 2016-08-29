# A Groovy CouchDB Client 

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
    compile 'de.gesellix:couchdb-client:2016-08-29_603362afaff5055b6bbe4dc29afa0f235cf2bb2e'
}
````

The integration test at `de.gesellix.couchdb.CouchDBClientIntegrationSpec` shows all supported use cases.

### Contributing

If you miss some feature or find an issue, please contact me via [issue tracker](https://github.com/gesellix/couchdb-client/issues).
If you'd like to contribute code, you're very welcome to submit a [pull request](https://github.com/gesellix/couchdb-client/pulls)! 

You may also contact me via Twitter [@gesellix](https://twitter.com/gesellix).
