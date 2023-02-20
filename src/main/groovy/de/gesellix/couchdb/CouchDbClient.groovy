package de.gesellix.couchdb

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.LocalDate

import static java.nio.charset.StandardCharsets.UTF_8
import static okhttp3.MediaType.parse

class CouchDbClient {

  private final static Logger log = LoggerFactory.getLogger(getClass())

  OkHttpClient client

  Json json

  boolean tlsEnabled
  String couchdbHost
  int couchdbPort

  String baseUrl

  String couchdbUsername
  String couchdbPassword

  CouchDbClient() {
    this(new JsonMoshi())
  }

  CouchDbClient(Json json) {
    this.client = new OkHttpClient()
    this.json = json
    this.tlsEnabled = false
    this.couchdbHost = "127.0.0.1"
    this.couchdbPort = 5984
  }

  void updateBaseUrl() {
    this.baseUrl = "${tlsEnabled ? "https" : "http"}://${couchdbHost}:${couchdbPort}"
  }

  String getBaseUrl() {
    if (baseUrl == null) {
      updateBaseUrl()
    }
    return baseUrl
  }

  void setTlsEnabled(boolean tlsEnabled) {
    this.tlsEnabled = tlsEnabled
    this.updateBaseUrl()
  }

  void setCouchdbHost(String couchdbHost) {
    this.couchdbHost = couchdbHost
    this.updateBaseUrl()
  }

  void setCouchdbPort(int couchdbPort) {
    this.couchdbPort = couchdbPort
    this.updateBaseUrl()
  }

  boolean healthy() {
    Request.Builder builder = new Request.Builder()
        .url(getBaseUrl())
        .get()
    Request request = builder.build()

    Response response = client.newCall(request).execute()

    if (!response.successful) {
      if (response.code() == 404) {
        response.body().close()
        return false
      }
      else {
        log.error("error {}", response.message())
        response.body().close()
        throw new IllegalStateException("could not check for database existence")
      }
    }
    else {
      log.info(response.body().string())
      return true
    }
  }

  def <R> R query(String db, String viewName, String key, boolean includeDocs = true) {
    List<String> query = []
    if (includeDocs) {
      query.add("include_docs=${includeDocs}")
    }
//    if (group) {
//      query.add("group=${group}")
//    }
    if (key) {
      String encodedKey = json.encodeQueryValue(key)
      query.add("key=${encodedKey}")
    }
    String queryAsString = query.join("&")

    Request.Builder builder = new Request.Builder()
        .url(getBaseUrl() +
             "/${db.toLowerCase()}" +
             "/_design/${db.capitalize()}" +
             "/_view/${viewName}" +
             "?${queryAsString}")
        .get()
    if (couchdbUsername && couchdbPassword) {
      builder = builder.header("Authorization", Credentials.basic(couchdbUsername, couchdbPassword))
    }
    Request request = builder.build()

    Response response = client.newCall(request).execute()
    if (!response.successful) {
      log.error("request failed: {}", request)
      throw new IOException(response.body().string())
    }

    def result = json.consume(response.body().byteStream(), Map)
    result.rows.collect { row -> includeDocs ? row.doc : row }
  }

  def <R> R query(String db, String viewName, Collection<String> keys, boolean includeDocs = true, boolean group = false) {
    List<String> query = []
    if (includeDocs) {
      query.add("include_docs=${includeDocs}")
    }
    if (group) {
      query.add("group=${group}")
    }
    if (keys) {
      String encodedKeys = json.encodeQueryValue(keys)
      query.add("keys=${encodedKeys}")
    }
    String queryAsString = query.join("&")

    Request.Builder builder = new Request.Builder()
        .url(getBaseUrl() +
             "/${db.toLowerCase()}" +
             "/_design/${db.capitalize()}" +
             "/_view/${viewName}" +
             "?${queryAsString}")
        .get()
    if (couchdbUsername && couchdbPassword) {
      builder = builder.header("Authorization", Credentials.basic(couchdbUsername, couchdbPassword))
    }
    Request request = builder.build()

    Response response = client.newCall(request).execute()
    if (!response.successful) {
      log.error("request failed: {}", request)
      throw new IOException(response.body().string())
    }

    def result = json.consume(response.body().byteStream(), Map)
    result.rows.collect { row -> includeDocs ? row.doc : row }
  }

  def getAllDocs(String db, boolean includeDocs = true, boolean includeDesignDoc = false) {
    Request.Builder builder = new Request.Builder()
        .url("${getBaseUrl()}/${db.toLowerCase()}" +
             "/_all_docs" +
             "?include_docs=${includeDocs}")
        .get()
    if (couchdbUsername && couchdbPassword) {
      builder = builder.header("Authorization", Credentials.basic(couchdbUsername, couchdbPassword))
    }
    Request request = builder.build()

    Response response = client.newCall(request).execute()

    if (!response.successful) {
      log.error("error getting all docs: {}/{}", response.code(), response.message())
      throw new IllegalStateException("could not get all docs with")
    }
    else {
      def allDocs = json.consume(response.body().byteStream(), Map)
      allDocs = allDocs.rows.collect { row -> includeDocs ? row.doc : row }
      if (!includeDesignDoc) {
        allDocs = allDocs.grep { !it._id?.startsWith("_design/") && !it.id?.startsWith("_design/") }
      }
      return allDocs
    }
  }

  def create(String db, Map document) {
    if (document == null) {
      throw new IllegalArgumentException("document may not be null")
    }
    if (document['_rev']) {
      log.error("document must be new, but has id({}), rev({})", document['_id'], document['_rev'])
      throw new IllegalArgumentException("document must be new")
    }

    beforeCreate(document)

    String documentAsJson = json.encodeDocument(document)
    RequestBody body = RequestBody.create(documentAsJson, parse("application/json"))

    Request.Builder builder = new Request.Builder()
    if (document['_id']) {
      String docId = document['_id']
      docId = sanitizeDocId(docId)
      builder = builder
          .url("${getBaseUrl()}/${db.toLowerCase()}/${docId}")
          .put(body)
    }
    else {
      builder = builder
          .url("${getBaseUrl()}/${db.toLowerCase()}")
          .post(body)
    }
    if (couchdbUsername && couchdbPassword) {
      builder = builder.header("Authorization", Credentials.basic(couchdbUsername, couchdbPassword))
    }
    Request request = builder.build()

    Response response = client.newCall(request).execute()

    Map result = json.consume(response.body().byteStream(), Map)
    if (!result.ok) {
      log.error("error {}", result)
      throw new IllegalStateException("error creating document")
    }

    Map createdDocument = json.decodeDocument(documentAsJson, Map.class)
    createdDocument['_id'] = result.id
    createdDocument['_rev'] = result.rev
    return createdDocument
  }

  def update(String db, Map document) {
    if (document == null) {
      throw new IllegalArgumentException("document may not be null")
    }
    if (!document['_id']) {
      throw new IllegalArgumentException("document id missing")
    }

    beforeUpdate(document)

    String documentAsJson = json.encodeDocument(document)
    RequestBody body = RequestBody.create(documentAsJson, parse("application/json"))

    def builder = new Request.Builder()
    String docId = document['_id']
    docId = sanitizeDocId(docId)
    builder = builder
        .url("${getBaseUrl()}/${db.toLowerCase()}/${docId}")
        .put(body)
    if (couchdbUsername && couchdbPassword) {
      builder = builder.header("Authorization", Credentials.basic(couchdbUsername, couchdbPassword))
    }
    Request request = builder.build()

    Response response = client.newCall(request).execute()

    Map result = json.consume(response.body().byteStream(), Map)
    if (!result.ok) {
      log.error("error {}", result)
      throw new IllegalStateException("error updating document")
    }

    Map updatedDocument = json.decodeDocument(documentAsJson, Map.class)
    updatedDocument['_rev'] = result.rev
    return updatedDocument
  }

  List<Map> updateBulk(String db, List<Map> documents) {
    if (documents == null) {
      throw new IllegalArgumentException("documents may not be null")
    }
    if (documents.empty) {
      throw new IllegalArgumentException("documents is empty")
    }

    documents.each { document ->
      if (document['_id']) {
        beforeUpdate(document)
      }
      else if (!document.dateCreated) {
        beforeCreate(document)
      }
    }

    Map updateDoc = [docs: documents]

    String documentAsJson = json.encodeDocument(updateDoc)
    RequestBody body = RequestBody.create(documentAsJson, parse("application/json"))

    Request.Builder builder = new Request.Builder()
    builder = builder
        .url("${getBaseUrl()}/${db.toLowerCase()}/_bulk_docs")
        .post(body)
    if (couchdbUsername && couchdbPassword) {
      builder = builder.header("Authorization", Credentials.basic(couchdbUsername, couchdbPassword))
    }
    Request request = builder.build()

    Response response = client.newCall(request).execute()
    if (!response.successful) {
      log.error("error {}", response.message())
      throw new IllegalStateException("bulk update failed")
    }

    List result = json.consume(response.body().byteStream(), List)
    [result, documents].transpose().each { updated, original ->
      if (updated.ok) {
        original._id = updated.id
        original._rev = updated.rev
        return original
      }
      else {
        log.error("error {}", updated)
        return null
      }
    }

    return result
  }

  boolean containsDb(String db) {
    Request.Builder builder = new Request.Builder()
        .url("${getBaseUrl()}/${db.toLowerCase()}")
        .head()
    if (couchdbUsername && couchdbPassword) {
      builder = builder.header("Authorization", Credentials.basic(couchdbUsername, couchdbPassword))
    }
    Request request = builder.build()

    Response response = client.newCall(request).execute()

    if (!response.successful) {
      if (response.code() == 404) {
        return false
      }
      else {
        log.error("error {}", response.message())
        throw new IllegalStateException("could not check for database existence")
      }
    }
    else {
      return true
    }
  }

  def createDb(String db) {
    Request.Builder builder = new Request.Builder()
        .url("${getBaseUrl()}/${db.toLowerCase()}")
        .put(RequestBody.create('', parse("application/json")))
    if (couchdbUsername && couchdbPassword) {
      builder = builder.header("Authorization", Credentials.basic(couchdbUsername, couchdbPassword))
    }
    Request request = builder.build()

    Response response = client.newCall(request).execute()

    Map result = json.consume(response.body().byteStream(), Map)
    if (!result.ok) {
      log.error("error {}", result)
      throw new IllegalStateException("error creating database")
    }
    return result
  }

  def deleteDb(String db) {
    Request.Builder builder = new Request.Builder()
        .url("${getBaseUrl()}/${db.toLowerCase()}")
        .delete()
    if (couchdbUsername && couchdbPassword) {
      builder = builder.header("Authorization", Credentials.basic(couchdbUsername, couchdbPassword))
    }
    Request request = builder.build()

    Response response = client.newCall(request).execute()

    Map result = json.consume(response.body().byteStream(), Map)
    if (!result.ok) {
      log.error("error {}", result)
      throw new IllegalStateException("error deleting database")
    }
    return result
  }

  Map createFindAllDocsView(String db) {
    String findAll = "function(doc) { emit(null, doc._id) }"
    return createView(db, "all", findAll, null)
  }

  Map createFindByPropertyView(String db, String propertyName) {
    String findByProperty = "function(doc) { if (doc['${propertyName}']) { emit(doc['${propertyName}'], doc._id) } }"
    return createView(db, "by_${propertyName}", findByProperty, null)
  }

  Map createView(String db, String viewName, String mapFunction, String reduceFunction) {
    Map view = [:]
    if (mapFunction) {
      view['map'] = mapFunction
    }
    if (reduceFunction) {
      view['reduce'] = reduceFunction
    }

    String designDocId = "_design/${db.capitalize()}"
    Map newDesignDoc = [
        _id     : designDocId,
        language: "javascript",
        views   : [
            (viewName): view
        ]
    ]

    boolean designDocExists = contains(db, designDocId)
    if (designDocExists) {
      Map currentDesignDoc = get(db, designDocId)
      Map mergedDesignDoc = merge(currentDesignDoc, newDesignDoc)
      if (mergedDesignDoc != currentDesignDoc) {
        update(db, mergedDesignDoc)
      }
      return get(db, designDocId)
    }
    else {
      return update(db, newDesignDoc)
    }
  }

  boolean contains(String db, String docId) {
    docId = sanitizeDocId(docId)
    Request.Builder builder = new Request.Builder()
        .url("${getBaseUrl()}/${db.toLowerCase()}/${docId}")
        .head()
    if (couchdbUsername && couchdbPassword) {
      builder = builder.header("Authorization", Credentials.basic(couchdbUsername, couchdbPassword))
    }
    Request request = builder.build()

    Response response = client.newCall(request).execute()

    if (!response.successful) {
      if (response.code() == 404) {
        return false
      }
      else {
        log.error("error {}", response.message())
        throw new IllegalStateException("could not check for doc existence")
      }
    }
    else {
      return true
    }
  }

  <R> R get(String db, String docId) {
    docId = sanitizeDocId(docId)
    Request.Builder builder = new Request.Builder()
        .url("${getBaseUrl()}/${db.toLowerCase()}/${docId}")
        .get()
    if (couchdbUsername && couchdbPassword) {
      builder = builder.header("Authorization", Credentials.basic(couchdbUsername, couchdbPassword))
    }
    Request request = builder.build()

    Response response = client.newCall(request).execute()

    if (!response.successful) {
      log.error("error getting document({}): {}/{}", docId, response.code(), response.message())
      throw new IllegalStateException("could not get doc with id '${docId}'")
    }
    else {
//      Type type = Types.newParameterizedType(List.class, String.class);
//      R doc = json.consume(response.body().byteStream(), Class<R>)
      R doc = json.consume(response.body().byteStream(), Map)
      return doc
    }
  }

  Map delete(String db, String docId, String rev) {
    docId = sanitizeDocId(docId)
    Request.Builder builder = new Request.Builder()
        .url("${getBaseUrl()}/${db.toLowerCase()}/${docId}?rev=${rev}")
        .delete()
    if (couchdbUsername && couchdbPassword) {
      builder = builder.header("Authorization", Credentials.basic(couchdbUsername, couchdbPassword))
    }
    Request request = builder.build()

    Response response = client.newCall(request).execute()

    if (!response.successful) {
      log.error("error deleting doc{ _id:{}, _rev:{} }: {}/{}", docId, rev, response.code(), response.message())
      throw new IllegalStateException("could not delete doc with id/rev '${docId}'/'${rev}'")
    }
    else {
      Map result = json.consume(response.body().byteStream(), Map)
      return result
    }
  }

  static Map merge(Map currentDoc, Map changedDoc) {
    def mergedDoc = currentDoc + [:]
    mergedDoc.views = (currentDoc.views ?: [:]) + (changedDoc.views ?: [:])
    return mergedDoc
  }

  static def sanitizeDocId(String docId) {
    if (!docId.startsWith('_')) {
      docId = URLEncoder.encode(docId, UTF_8.toString())
    }
    docId
  }

  void beforeCreate(Map document) {
    if (!document.dateCreated) {
      document.dateCreated = LocalDate.now().toString()
    }
  }

  void beforeUpdate(Map document) {
    document.dateUpdated = LocalDate.now().toString()
  }
}
