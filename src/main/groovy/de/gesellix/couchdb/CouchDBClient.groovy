package de.gesellix.couchdb

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.joda.time.LocalDate

import static java.nio.charset.StandardCharsets.UTF_8
import static okhttp3.MediaType.parse

@Slf4j
class CouchDBClient {

    OkHttpClient client

    ObjectMapper objectMapper

    String couchdbHost
    int couchdbPort

    String couchdbUsername
    String couchdbPassword

    CouchDBClient() {
        this.client = new OkHttpClient()
        this.objectMapper = new ObjectMapper()
        this.couchdbHost = "127.0.0.1"
        this.couchdbPort = 5984
    }

    def <R> R query(String db, String viewName, String key, boolean includeDocs = true) {
        def encodedKey = objectMapper.writeValueAsString(key)

        Request request = new Request.Builder()
                .url("http://${couchdbHost}:${couchdbPort}" +
                "/${db.toLowerCase()}" +
                "/_design/${db.capitalize()}" +
                "/_view/${viewName}" +
                "?include_docs=${includeDocs}" +
                "&key=${encodedKey}")
                .build()

        def response = client.newCall(request).execute()
        if (!response.successful) {
            throw new IOException(response.body().string())
        }

        def result = new JsonSlurper().parse(response.body().byteStream())
        result.rows.collect { row -> includeDocs ? row.doc : row }
    }

    def <R> R query(String db, String viewName, Collection<String> keys, boolean includeDocs = true) {
        def encodedKeys = objectMapper.writeValueAsString(keys)

        Request request = new Request.Builder()
                .url("http://${couchdbHost}:${couchdbPort}" +
                "/${db.toLowerCase()}" +
                "/_design/${db.capitalize()}" +
                "/_view/${viewName}" +
                "?include_docs=${includeDocs}" +
                "&keys=${encodedKeys}")
                .build()

        def response = client.newCall(request).execute()
        if (!response.successful) {
            throw new IOException(response.body().string())
        }

        def result = new JsonSlurper().parse(response.body().byteStream())
        result.rows.collect { row -> includeDocs ? row.doc : row }
    }

    def create(String db, def document) {
        if (document == null) {
            throw new IllegalArgumentException("document may not be null")
        }
        if (document['_rev']) {
            log.error("document must be new, but has id({}), rev({})", document['_id'], document['_rev'])
            throw new IllegalArgumentException("document must be new")
        }

        if (!document.dateCreated) {
            document.dateCreated = new LocalDate()
        }

        def documentAsJson = objectMapper.writeValueAsString(document)
        def body = RequestBody.create(parse("application/json"), documentAsJson)

        def builder = new Request.Builder()
        if (document['_id']) {
            String docId = document['_id']
            if (!docId.startsWith('_')) {
                docId = URLEncoder.encode(docId, UTF_8.toString())
            }
            builder = builder
                    .url("http://${couchdbHost}:${couchdbPort}/${db.toLowerCase()}/${docId}")
                    .put(body)
        } else {
            builder = builder
                    .url("http://${couchdbHost}:${couchdbPort}/${db.toLowerCase()}")
                    .post(body)
        }
        def request = builder.build()

        def response = client.newCall(request).execute()

        def result = new JsonSlurper().parse(response.body().byteStream())
        if (!result.ok) {
            log.error("error {}", result)
            throw new IllegalStateException("error creating document")
        }

        def createdDocument = objectMapper.readValue(documentAsJson, Map)
        createdDocument['_id'] = result.id
        createdDocument['_rev'] = result.rev
        return createdDocument
    }

    def update(String db, def document) {
        if (document == null) {
            throw new IllegalArgumentException("document may not be null")
        }
        if (!document['_id']) {
            throw new IllegalArgumentException("document id missing")
        }

        document.dateUpdated = new LocalDate()

        def documentAsJson = objectMapper.writeValueAsString(document)
        def body = RequestBody.create(parse("application/json"), documentAsJson)

        def builder = new Request.Builder()
        String docId = document['_id']
        if (!docId.startsWith('_')) {
            docId = URLEncoder.encode(docId, UTF_8.toString())
        }
        builder = builder
                .url("http://${couchdbHost}:${couchdbPort}/${db.toLowerCase()}/${docId}")
                .put(body)
        def request = builder.build()

        def response = client.newCall(request).execute()

        def result = new JsonSlurper().parse(response.body().byteStream())
        if (!result.ok) {
            log.error("error {}", result)
            throw new IllegalStateException("error updating document")
        }

        def updatedDocument = objectMapper.readValue(documentAsJson, Map)
        updatedDocument['_rev'] = result.rev
        return updatedDocument
    }
}
