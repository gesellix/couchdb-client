package de.gesellix.couchdb

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import de.gesellix.couchdb.moshi.LocalDateJsonAdapter
import de.gesellix.couchdb.moshi.MoshiJson
import de.gesellix.couchdb.moshi.MoshiViewQueryResponse
import de.gesellix.couchdb.moshi.NestedRevisionAdapter
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import java.lang.reflect.Type
import java.time.LocalDate

@Testcontainers
@Stepwise
class CouchDbClientIntegrationSpec extends Specification {

  static final int COUCHDB_PORT = 5984
//    static final String COUCHDB_IMAGE = "couchdb:1.7.1"
  static final String COUCHDB_IMAGE = "couchdb:3.3.1"
  static GenericContainer couchdbContainer = new GenericContainer(COUCHDB_IMAGE)
      .withEnv([
          COUCHDB_USER    : System.env['couchdb.username'] as String ?: "admin",
          COUCHDB_PASSWORD: System.env['couchdb.password'] as String ?: "admin"])
      .withExposedPorts(COUCHDB_PORT)
      .waitingFor(Wait.forHttp("/_up"))

  // allows @Testcontainers to find the statically initialized container reference
  @Shared
  GenericContainer couchdb = couchdbContainer

  @Shared
  CouchDbClient client
  @Shared
  String database

  def setupSpec() {
    client = new CouchDbClient(
        new MoshiJson(
            new Moshi.Builder()
                .add(LocalDate, new LocalDateJsonAdapter())
                .add(new NestedRevisionAdapter())))
    client.couchdbHost = System.env['couchdb.host'] ?: couchdbContainer.host
    client.couchdbPort = System.env['couchdb.port'] ?: couchdbContainer.getMappedPort(COUCHDB_PORT)
    client.couchdbUsername = System.env['couchdb.username'] ?: "admin"
    client.couchdbPassword = System.env['couchdb.password'] ?: "admin"
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
    Map existingDoc = client.create(database, [:])

    when:
    Map result = client.get(database, existingDoc._id as String)

    then:
    result._id == existingDoc._id
    and:
    result._rev.startsWith("1-")
  }

  def "delete doc"() {
    given:
    Map existingDoc = client.create(database, [:])
    boolean created = client.contains(database, existingDoc._id as String)

    when:
    Map result = client.delete(database, existingDoc._id as String, existingDoc._rev as String)

    then:
    created
    and:
    result.ok == true
    and:
    !client.contains(database, existingDoc._id as String)
  }

  def "create some documents"() {
    given:
    String docId1 = "test-id/${UUID.randomUUID()}"
    String docId2 = "test-id/${UUID.randomUUID()}"

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
    String docId1 = "test-id/${UUID.randomUUID()}"
    String docId2 = "test-id/${UUID.randomUUID()}"
    String docId3 = "test-id/${UUID.randomUUID()}"

    client.create(database, [_id: docId1, 'a-property': "doc-1"])
    client.create(database, [_id: docId2, 'a-property': "doc-2"])

    Map doc1 = client.get(database, docId1)
    doc1.'a-property' = 'doc-1 changed'
    doc1.'a-new-property' = 'a new property'

    Map doc2 = client.get(database, docId2)
    doc2.'a-property' = 'doc-2 also changed'

    Map doc3 = [_id: docId3, 'a-property': 'doc-3 via bulk']

    when:
    List<Map> originalDocs = [doc1, doc2, doc3]
    List<Map> bulkResult = client.updateBulk(database, originalDocs)

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
    String docId = "test-id/${UUID.randomUUID()}"
    client.create(database, [_id: docId, 'a-property': "query view with single key-1"])

    when:
    List<Map> result = client.query(database, "by_a-property", "query view with single key-1")

    then:
    result.first()._id == docId
  }

  def "query view with multiple keys"() {
    given:
    String docId1 = "test-id/${UUID.randomUUID()}"
    client.create(database, [_id: docId1, 'a-property': "a-value-1"])
    String docId2 = "test-id/${UUID.randomUUID()}"
    client.create(database, [_id: docId2, 'a-property': "a-value-2"])

    when:
    List<Map> result = client.query(database, "by_a-property", ["a-value-1", "unknown"])

    then:
    result._id == [docId1]
  }

  def "add a view with map and reduce"() {
    when:
    def viewMap = "function(doc) { if (doc['title']) { emit(doc['title'], doc._id) } }"
    def viewReduce = "function(keys, values, rereduce) { return true }"
    def result = client.createView(database, "suggestions", viewMap, viewReduce)

    then:
    result._id == "_design/${database.capitalize()}"
    and:
    // ensure that the new view has been merged into the existing design doc
    result.views["by_a-property"]
    and:
    result.views["suggestions"].map == viewMap
    and:
    result.views["suggestions"].reduce == viewReduce
  }

  def "query unique keys"() {
    given:
    String docId1 = "test-id/${UUID.randomUUID()}"
    client.create(database, [_id: docId1, 'a-property': "a-value-1", 'title': "A not so unique title"])
    String docId2 = "test-id/${UUID.randomUUID()}"
    client.create(database, [_id: docId2, 'a-property': "a-value-2", 'title': "A quite unique title"])
    String docId3 = "test-id/${UUID.randomUUID()}"
    client.create(database, [_id: docId3, 'a-property': "a-value-3", 'title': "A not so unique title"])

    when:
    List<Map<String, ?>> result = client.query(database, "suggestions", null, false, true)

    then:
    result.size() == 2
    and:
    result.collect { it.get('key') }.sort() == [
        "A not so unique title",
        "A quite unique title"
    ].sort()
  }

  def "iterate over all documents, includeDocs = false"() {
    given:
    String dbName = "database2"
    client.createDb(dbName)

    Type docType = Types.newParameterizedType(Map, String, Object)
    Type resultType = Types.newParameterizedType(MoshiViewQueryResponse, docType)

    int docCount = client.getDbInfo(dbName).doc_count
    int max = 10
    (1..max).each {
      client.create(dbName, ["position": "ranged/$it".toString()])
    }

    boolean includeDocs = false
    int pageSize = 4

    when:
    ViewQueryResponse<Map> docs1 = client.getAllDocs(resultType, dbName, null, pageSize + 1, includeDocs, false)
    ViewQueryResponse<Map> docs2 = client.getAllDocs(resultType, dbName, docs1.rows.last().id, pageSize + 1, includeDocs, false)
    ViewQueryResponse<Map> docs3 = client.getAllDocs(resultType, dbName, docs2.rows.last().id, pageSize + 1, includeDocs, false)
    println("docs1: $docs1")
    println("docs2: $docs2")
    println("docs3: $docs3")

    then:
    docs1.totalRows == docCount + max
    docs1.offset == 0 * pageSize
    docs1.rows.size() == pageSize + 1
    docs1.rows.first().id != null
    docs1.rows.first().rev != null
    and:
    docs2.totalRows == docCount + max
    docs2.offset == 1 * pageSize
    docs2.rows.size() == pageSize + 1
    docs2.rows.first().id == docs1.rows.last().id
    docs2.rows.first().rev == docs1.rows.last().rev
    and:
    docs3.totalRows == docCount + max
    docs3.offset == 2 * pageSize
    docs3.rows.size() == (docs3.totalRows - docs3.offset) % pageSize
    docs3.rows.first().id == docs2.rows.last().id
    docs3.rows.first().rev == docs2.rows.last().rev

    cleanup:
    client.deleteDb(dbName)
  }

  def "iterate over all documents, includeDocs=true"() {
    given:
    String dbName = "database3"
    client.createDb(dbName)

    Type docType = Types.newParameterizedType(Map, String, Object)
    Type resultType = Types.newParameterizedType(MoshiViewQueryResponse, docType)

    int docCount = client.getDbInfo(dbName).doc_count
    int max = 10
    (1..max).each {
      client.create(dbName, ["position": "ranged/$it".toString()])
    }

    boolean includeDocs = true
    int pageSize = 4

    when:
    ViewQueryResponse<Map> docs1 = client.getAllDocs(resultType, dbName, null, pageSize + 1, includeDocs, false)
    ViewQueryResponse<Map> docs2 = client.getAllDocs(resultType, dbName, docs1.rows.last().id, pageSize + 1, includeDocs, false)
    ViewQueryResponse<Map> docs3 = client.getAllDocs(resultType, dbName, docs2.rows.last().id, pageSize + 1, includeDocs, false)
    println("docs1: $docs1")
    println("docs2: $docs2")
    println("docs3: $docs3")

    then:
    docs1.totalRows == docCount + max
    docs1.offset == 0 * pageSize
    docs1.rows.size() == pageSize + 1
    docs1.rows.first().id != null
    docs1.rows.first().rev != null
    docs1.rows.first().doc._rev == docs1.rows.first().rev
    and:
    docs2.totalRows == docCount + max
    docs2.offset == 1 * pageSize
    docs2.rows.size() == pageSize + 1
    docs2.rows.first().id == docs1.rows.last().id
    docs2.rows.first().rev == docs1.rows.last().rev
    docs2.rows.first().doc._rev == docs1.rows.last().rev
    and:
    docs3.totalRows == docCount + max
    docs3.offset == 2 * pageSize
    docs3.rows.size() == (docs3.totalRows - docs3.offset) % pageSize
    docs3.rows.first().id == docs2.rows.last().id
    docs3.rows.first().rev == docs2.rows.last().rev
    docs3.rows.first().doc._rev == docs2.rows.last().rev

    cleanup:
    client.deleteDb(dbName)
  }

  def "delete test database"() {
    when:
    client.deleteDb(database)
    then:
    !client.containsDb(database)
  }
}
