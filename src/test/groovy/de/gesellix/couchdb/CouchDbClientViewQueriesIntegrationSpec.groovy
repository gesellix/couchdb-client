package de.gesellix.couchdb


import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import de.gesellix.couchdb.model.MapWithDocumentId
import de.gesellix.couchdb.moshi.LocalDateJsonAdapter
import de.gesellix.couchdb.moshi.MapWithDocumentIdAdapter
import de.gesellix.couchdb.moshi.MoshiAllDocsViewQueryResponse
import de.gesellix.couchdb.moshi.MoshiJson
import de.gesellix.couchdb.moshi.MoshiReducedViewQueryResponse
import de.gesellix.couchdb.moshi.MoshiViewQueryResponse
import de.gesellix.couchdb.moshi.NestedRevisionAdapter
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import java.time.LocalDate

@Testcontainers
@Stepwise
class CouchDbClientViewQueriesIntegrationSpec extends Specification {

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
                .add(new NestedRevisionAdapter())
                .add(new MapWithDocumentIdAdapter())
                .add(new RowWithAuthorAdapter())))
    client.couchdbHost = System.env['couchdb.host'] ?: couchdbContainer.host
    client.couchdbPort = System.env['couchdb.port'] ?: couchdbContainer.getMappedPort(COUCHDB_PORT)
    client.couchdbUsername = System.env['couchdb.username'] ?: "admin"
    client.couchdbPassword = System.env['couchdb.password'] ?: "admin"

    prepareViews()
    createDocuments()
  }

  def cleanupSpec() {
    if (couchdbContainer.isRunning()) {
      !client.containsDb(database) || client.deleteDb(database)
    }
  }

  void prepareViews() {
    database = "test-db-${UUID.randomUUID()}"
    client.createDb(database)
    client.createFindByPropertyView(database, "author")
    client.createView(database,
        "quotes-by-author",
        "function(doc) { if (doc['author']) { emit(doc['author'], doc._id) } }",
        "function(keys, values, rereduce) { return true }")
  }

  void createDocuments() {
    List<Map<String, String>> quotes = new MoshiJson().consume(getClass().getResourceAsStream("/quotes.json"), Types.newParameterizedType(
        List, Types.newParameterizedType(Map, String, String)
    ))
    client.updateBulk(database, quotes)
  }

  def "debug info"() {
    expect:
    String allDocs = "${client.getCurlCommandLine("/" + database + "/_all_docs?limit=2&include_docs=true")}"
    String quotesByAuthor = "${client.getCurlCommandLine("/" + database + "/_design/${database.capitalize()}/_view/quotes-by-author?limit=2&include_docs=true&reduce=false")}"
    String quotesByAuthorReduced = "${client.getCurlCommandLine("/" + database + "/_design/${database.capitalize()}/_view/quotes-by-author?limit=2&reduce=true&group=true")}"
    println(allDocs)
    println(quotesByAuthor)
    println(quotesByAuthorReduced)
  }

  def "query view with single key"() {
    given:
    def author = "Johann Wolfgang von Goethe"
    def expectedQuote = "In the end we retain from our studies only that which we practically apply."

    when:
    List<Map> result = client.query(database, "by_author", author)

    then:
    result.size() == 16
    and:
    result.every { it.author == author }
    and:
    result.find { it.text == expectedQuote }
  }

  def "query view with multiple keys"() {
    given:
    def author = "Johann Wolfgang von Goethe"
    def expectedQuote = "In the end we retain from our studies only that which we practically apply."

    when:
    List<Map> result = client.query(database, "by_author", [author, "unknown-key"])

    then:
    result.size() == 16
    and:
    result.every { it.author == author }
    and:
    result.find { it.text == expectedQuote }
  }

  def "page /_all_docs"() {
    given:
    def pageSize = 11
    def resultType = Types.newParameterizedType(
        MoshiAllDocsViewQueryResponse, Types.newParameterizedType(
        MapWithDocumentId, Object))

    when:
    MoshiAllDocsViewQueryResponse<MapWithDocumentId> page1 = client.getAllDocs(
        resultType, database, null, pageSize, true, true)
    MoshiAllDocsViewQueryResponse<MapWithDocumentId> page2 = client.getAllDocs(
        resultType, database, page1.rows.last().docId, pageSize, true, true)

    then:
    page1.totalRows == 1645
    page1.rows.size() == pageSize
    page1.rows.first().docId =~ ".+"
    page2.totalRows == page2.totalRows
    page2.rows.size() == pageSize
    page1.rows.last().docId == page2.rows.first().docId
  }

  def "page /_view/a-view, reduce=false"() {
    given:
    String designDocId = "_design/${database.capitalize()}"
    def pageSize = 11
    def resultType = Types.newParameterizedType(
        MoshiViewQueryResponse, String, String, Types.newParameterizedType(
        MapWithDocumentId, String))

    when:
    MoshiViewQueryResponse<String, String, MapWithDocumentId<String>> page1 = client.queryPage(
        resultType, database, designDocId, "quotes-by-author", false,
        null, null, null, pageSize, true, false)
    MoshiViewQueryResponse<String, String, MapWithDocumentId<String>> page2 = client.queryPage(
        resultType, database, designDocId, "quotes-by-author", false,
        page1.rows.last().key, page1.rows.last().docId, null, pageSize, true, false)

    then:
    page1.totalRows == 1548
    page1.rows.size() == pageSize
    page2.totalRows == page2.totalRows
    page2.rows.size() == pageSize
    page1.rows.first().key =~ ".+"
    page1.rows.first().docId =~ ".+"
    page1.rows.first().key != page1.rows.last().key
    page1.rows.last().key == page2.rows.first().key
    page1.rows.last().docId == page2.rows.first().docId
  }

  def "page /_view/a-view, reduce=true"() {
    given:
    String designDocId = "_design/${database.capitalize()}"
    def pageSize = 11
    def resultType = Types.newParameterizedType(
        MoshiReducedViewQueryResponse, String, RowWithAuthor)

    when:
    MoshiReducedViewQueryResponse<String, RowWithAuthor> page1 = client.queryPage(
        resultType, database, designDocId, "quotes-by-author", true,
        null, null, null, pageSize, false, false)
    MoshiReducedViewQueryResponse<String, RowWithAuthor> page2 = client.queryPage(
        resultType, database, designDocId, "quotes-by-author", true,
        page1.rows.last().getKey(), null, null, pageSize, false, false)

    then:
    page1.rows.size() == pageSize
    page2.rows.size() == pageSize
    page1.rows.first().getKey() == "A. A. Milne"
    page2.rows.first().getKey() == "Alan Watts"
    page1.rows.last().getKey() == page2.rows.first().getKey()
  }
}
