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
                couchdbPort: 5984)
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
}
