package de.gesellix.couchdb

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import okhttp3.OkHttpClient
import org.joda.time.LocalDate
import spock.lang.Specification

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS

class CouchDBClientSpec extends Specification {

    CouchDBClient client

    def setup() {
        client = new CouchDBClient(
                client: new OkHttpClient(),
                objectMapper: newObjectMapper(),
                couchdbHost: "192.168.99.100",
                couchdbPort: 5984,
                couchdbUsername: "foo",
                couchdbPassword: "bar")
    }

    ObjectMapper newObjectMapper() {
        def objectMapper = new ObjectMapper()
        objectMapper.registerModule(new JodaModule())
        objectMapper.configure(WRITE_DATES_AS_TIMESTAMPS, false)
        objectMapper
    }

    def "query view with single key"() {
        when:
        def result = client.query("topicProUser", "by_username", "tobias@gesellix.de")

        then:
        result.first()._id == "gesellix"
    }

    def "query view with multiple keys"() {
        when:
        def result = client.query("themen", "byMedienId", ["3", "unknown-id"])

        then:
        result.unique { thema -> thema.medienId }.medienId == ["3"]
    }

    def "create doc with existing _id"() {
        given:
        def docId = "test-id/${UUID.randomUUID()}".toString()
        def today = new LocalDate()

        when:
        def result = client.create("test-db", [_id: docId])

        then:
        result._id == docId
        and:
        result._rev.startsWith("1-")
        and:
        result.dateCreated
        and:
        today.minusDays(1).isBefore(new LocalDate(result.dateCreated as String))
        and:
        today.plusDays(1).isAfter(new LocalDate(result.dateCreated as String))
    }

    def "create doc with missing _id"() {
        given:
        def today = new LocalDate()

        when:
        def result = client.create("test-db", [:])

        then:
        result._id =~ "\\w+"
        and:
        result._rev.startsWith("1-")
        and:
        result.dateCreated
        and:
        today.minusDays(1).isBefore(new LocalDate(result.dateCreated as String))
        and:
        today.plusDays(1).isAfter(new LocalDate(result.dateCreated as String))
    }

    def "update doc"() {
        given:
        def today = new LocalDate()
        def existingDoc = client.create("test-db", [:])

        when:
        def result = client.update("test-db", [_id: existingDoc._id, _rev: existingDoc._rev])

        then:
        result._id =~ "\\w+"
        and:
        result._rev.startsWith("2-")
        and:
        !existingDoc.dateUdated
        and:
        result.dateUpdated
        and:
        today.minusDays(1).isBefore(new LocalDate(result.dateUpdated as String))
        and:
        today.plusDays(1).isAfter(new LocalDate(result.dateUpdated as String))
    }

    def "get doc"() {
        given:
        def existingDoc = client.create("test-db", [:])

        when:
        def result = client.get("test-db", existingDoc._id as String)

        then:
        result._id == existingDoc._id
        and:
        result._rev.startsWith("1-")
    }

    def "delete doc"() {
        given:
        def existingDoc = client.create("test-db", [:])
        def created = client.contains("test-db", existingDoc._id as String)

        when:
        def result = client.delete("test-db", existingDoc._id as String, existingDoc._rev as String)

        then:
        created
        and:
        result.ok == true
        and:
        !client.contains("test-db", existingDoc._id as String)
    }

    def "create and delete a database"() {
        when:
        def result = client.createDb("test-db-delete-me")

        then:
        result == [ok: true]

        cleanup:
        client.deleteDb("test-db-delete-me")
    }

    def "check for non-existing database"() {
        when:
        def exists = client.containsDb("test-db-${UUID.randomUUID()}")

        then:
        !exists
    }

    def "check for existing database"() {
        given:
        def dbName = "test-db-${UUID.randomUUID()}"
        client.createDb(dbName)

        when:
        def exists = client.containsDb(dbName)

        then:
        exists

        cleanup:
        client.deleteDb(dbName)
    }

    def "create view"() {
        when:
        def result = client.createFindByPropertyView("test-db", "a-property")

        then:
        result._id == "_design/${"test-db".capitalize()}"
        and:
        result.views["by_a-property"].map == "function(doc) { if (doc.a-property) { emit(doc.a-property, doc._id) } }"
        and:
        !result.views["by_a-property"].reduce
    }
}
