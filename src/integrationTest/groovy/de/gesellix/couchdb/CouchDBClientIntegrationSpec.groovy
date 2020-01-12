package de.gesellix.couchdb

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import java.time.LocalDate

@Testcontainers
@Stepwise
class CouchDBClientIntegrationSpec extends Specification {

    static final int COUCHDB_PORT = 5984
//    static final String COUCHDB_IMAGE = "couchdb:1.7.1"
    static final String COUCHDB_IMAGE = "couchdb:2.3.1"
    static GenericContainer couchdbContainer = new GenericContainer(COUCHDB_IMAGE)
            .withExposedPorts(COUCHDB_PORT)
//            .waitingFor(Wait.forHttp("/"))
            .waitingFor(Wait.forHttp("/_up"))

    // allows @Testcontainers to find the statically initialized container reference
    @Shared
    GenericContainer couchdb = couchdbContainer

    @Shared
    CouchDBClient client
    @Shared
    String database

    def setupSpec() {
        client = new CouchDBClient(
                client: new OkHttpClient(),
                moshi: new Moshi.Builder()
                        .add(LocalDate, new LocalDateJsonAdapter())
                        .build(),
                couchdbHost: System.env['couchdb.host'] ?: couchdbContainer.containerIpAddress,
                couchdbPort: System.env['couchdb.port'] ?: couchdbContainer.getMappedPort(COUCHDB_PORT),
                couchdbUsername: System.env['couchdb.username'] ?: null,
                couchdbPassword: System.env['couchdb.password'] ?: null)
        database = "test-db-${UUID.randomUUID()}"
    }

    def cleanupSpec() {
        if (couchdbContainer.isRunning()) {
            !client.containsDb(database) || client.deleteDb(database)
        }
    }

    def "ensure connectivity"() {
        expect:
        client.healthy()
    }

    def "verify non-existing test database"() {
        expect:
        !client.containsDb(database)
    }

    def "create test database"() {
        when:
        client.createDb(database)
        then:
        client.containsDb(database)
    }

    def "create view"() {
        when:
        def result = client.createFindByPropertyView(database, "a-property")

        then:
        result._id == "_design/${database.capitalize()}"
        and:
        result.views["by_a-property"].map == "function(doc) { if (doc['a-property']) { emit(doc['a-property'], doc._id) } }"
        and:
        !result.views["by_a-property"].reduce
    }

    def "create doc with existing _id"() {
        given:
        def docId = "test-id/${UUID.randomUUID()}".toString()
        def today = LocalDate.now()

        when:
        def result = client.create(database, [_id: docId, exampleDate: LocalDate.parse("2020-01-31")])

        then:
        result._id == docId
        and:
        result._rev.startsWith("1-")
        and:
        result.exampleDate == "2020-01-31"
        and:
        result.dateCreated
        and:
        today.minusDays(1).isBefore(LocalDate.parse(result.dateCreated as String))
        and:
        today.plusDays(1).isAfter(LocalDate.parse(result.dateCreated as String))
    }

    def "create doc with missing _id"() {
        given:
        def today = LocalDate.now()

        when:
        def result = client.create(database, [:])

        then:
        result._id =~ "\\w+"
        and:
        result._rev.startsWith("1-")
        and:
        result.dateCreated
        and:
        today.minusDays(1).isBefore(LocalDate.parse(result.dateCreated as String))
        and:
        today.plusDays(1).isAfter(LocalDate.parse(result.dateCreated as String))
    }

    def "update doc"() {
        given:
        def today = LocalDate.now()
        def existingDoc = client.create(database, [:])

        when:
        def result = client.update(database, [_id: existingDoc._id, _rev: existingDoc._rev, exampleDate: LocalDate.parse("2020-02-02")])

        then:
        result._id =~ "\\w+"
        and:
        result._rev.startsWith("2-")
        and:
        result.exampleDate == "2020-02-02"
        and:
        !existingDoc.dateUdated
        and:
        result.dateUpdated
        and:
        today.minusDays(1).isBefore(LocalDate.parse(result.dateUpdated as String))
        and:
        today.plusDays(1).isAfter(LocalDate.parse(result.dateUpdated as String))
    }

    def "get doc"() {
        given:
        def existingDoc = client.create(database, [:])

        when:
        def result = client.get(database, existingDoc._id as String)

        then:
        result._id == existingDoc._id
        and:
        result._rev.startsWith("1-")
    }

    def "delete doc"() {
        given:
        def existingDoc = client.create(database, [:])
        def created = client.contains(database, existingDoc._id as String)

        when:
        def result = client.delete(database, existingDoc._id as String, existingDoc._rev as String)

        then:
        created
        and:
        result.ok == true
        and:
        !client.contains(database, existingDoc._id as String)
    }

    def "create some documents"() {
        given:
        def docId1 = "test-id/${UUID.randomUUID()}".toString()
        def docId2 = "test-id/${UUID.randomUUID()}".toString()

        when:
        client.create(database, [_id: docId1, 'a-property': "create some documents-1"])
        and:
        client.create(database, [_id: docId2, 'a-property': "create some documents-2"])

        then:
        client.get(database, docId1).'a-property' == "create some documents-1"
        and:
        client.get(database, docId2).'a-property' == "create some documents-2"
    }

    def "update documents in bulk"() {
        given:
        def docId1 = "test-id/${UUID.randomUUID()}".toString()
        def docId2 = "test-id/${UUID.randomUUID()}".toString()
        def docId3 = "test-id/${UUID.randomUUID()}".toString()

        client.create(database, [_id: docId1, 'a-property': "doc-1"])
        client.create(database, [_id: docId2, 'a-property': "doc-2"])

        def doc1 = client.get(database, docId1)
        doc1.'a-property' = 'doc-1 changed'
        doc1.'a-new-property' = 'a new property'

        def doc2 = client.get(database, docId2)
        doc2.'a-property' = 'doc-2 also changed'

        def doc3 = [_id: docId3, 'a-property': 'doc-3 via bulk']

        when:
        def originalDocs = [doc1, doc2, doc3]
        def bulkResult = client.updateBulk(database, originalDocs)

        then:
        doc1 == client.get(database, docId1)
        doc2 == client.get(database, docId2)
        doc3 == client.get(database, docId3)
        and:
        bulkResult.size() == 3
        and:
        bulkResult.each { it.ok == true }
    }

    def "query view with single key"() {
        given:
        def docId = "test-id/${UUID.randomUUID()}".toString()
        client.create(database, [_id: docId, 'a-property': "query view with single key-1"])

        when:
        def result = client.query(database, "by_a-property", "query view with single key-1")

        then:
        result.first()._id == docId
    }

    def "query view with multiple keys"() {
        given:
        def docId1 = "test-id/${UUID.randomUUID()}".toString()
        client.create(database, [_id: docId1, 'a-property': "a-value-1"])
        def docId2 = "test-id/${UUID.randomUUID()}".toString()
        client.create(database, [_id: docId2, 'a-property': "a-value-2"])

        when:
        def result = client.query(database, "by_a-property", ["a-value-1", "unknown"])

        then:
        result._id == [docId1]
    }

    def "delete test database"() {
        when:
        client.deleteDb(database)
        then:
        !client.containsDb(database)
    }
}
