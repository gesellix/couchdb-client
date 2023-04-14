package de.gesellix.couchdb

import de.gesellix.couchdb.model.NonReducedViewQueryResponse
import de.gesellix.couchdb.model.RowReference
import groovy.transform.PackageScope
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.Type
import java.time.LocalDate

import static java.nio.charset.StandardCharsets.UTF_8
import static okhttp3.MediaType.parse

class CouchDbClient {

  private final static Logger log = LoggerFactory.getLogger(getClass())

  OkHttpClient client

  int MAX_QUERY_KEY_LENGTH = 5000
  Json json

  boolean tlsEnabled
  String couchdbHost
  int couchdbPort

  String baseUrl

  String couchdbUsername
  String couchdbPassword

  CouchDbClient(Json json) {
    this.client = new OkHttpClient()
    this.json = json
    this.tlsEnabled = false
    this.couchdbHost = "127.0.0.1"
    this.couchdbPort = 5984
  }

  @PackageScope
  String getCurlCommandLine(String suffix) {
    String authorization = ""
    if (couchdbUsername && couchdbPassword) {
      authorization = "-H \"Authorization: ${Credentials.basic(couchdbUsername, couchdbPassword)}\""
    }
    return "curl ${authorization} \"${getBaseUrl()}${suffix ?: ""}\""
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
      } else {
        log.error("error {}", response.message())
        response.body().close()
        throw new IllegalStateException("could not check for database existence")
      }
    } else {
      log.info(response.body().string())
      return true
    }
  }

  def <R> R query(String db, String viewName, String key, boolean includeDocs = true) {
    String designDocId = "_design/${db.capitalize()}"
    return query(db, designDocId, viewName, key, includeDocs)
  }

  def <R> R query(String db, String designDocId, String viewName, String key, boolean includeDocs = true) {
    List<String> query = []
    if (includeDocs) {
      query.add("include_docs=${includeDocs}")
    }
//    if (group) {
//      query.add("group=${group}")
//    }
    Map postBody = [:]
    boolean doPost = false
    if (key) {
      String encodedKey = urlEncode(json.encodeQueryValue(key))
      if (encodedKey.length() > MAX_QUERY_KEY_LENGTH) {
        doPost = true
        postBody['key'] = key
      } else {
        query.add("key=${encodedKey}")
      }
    }
    String queryAsString = query.join("&")

    Request.Builder builder = new Request.Builder()
        .url(getBaseUrl() +
            "/${db.toLowerCase()}" +
            "/${designDocId}" +
            "/_view/${viewName}" +
            "?${queryAsString}")
    if (doPost) {
      String documentAsJson = json.encodeDocument(postBody)
      builder = builder.post(RequestBody.create(documentAsJson, parse("application/json")))
    } else {
      builder = builder.get()
    }
    if (couchdbUsername && couchdbPassword) {
      builder = builder.header("Authorization", Credentials.basic(couchdbUsername, couchdbPassword))
    }
    Request request = builder.build()

    Response response = client.newCall(request).execute()
    if (!response.successful) {
      if (response.body().contentLength() > 0) {
        log.error("error querying view: {}/{}: {}", response.code(), response.message(), response.body().string())
      } else {
        log.error("error querying view: {}/{}", response.code(), response.message())
      }
      throw new IllegalStateException("could not query view")
    }

    def result = json.consume(response.body().byteStream(), Map)
    result.rows.collect { row -> includeDocs ? row.doc : row }
  }

  def <R> R query(String db, String viewName, Collection<String> keys, boolean includeDocs = true, boolean group = false) {
    String designDocId = "_design/${db.capitalize()}"
    return query(db, designDocId, viewName, keys, includeDocs, group)
  }

  def <R> R query(String db, String designDocId, String viewName, Collection<String> keys, boolean includeDocs = true, boolean group = false) {
    List<String> query = []
    if (includeDocs) {
      query.add("include_docs=${includeDocs}")
    }
    if (group) {
      query.add("group=${group}")
    }
    Map postBody = [:]
    boolean doPost = false
    if (keys) {
      String encodedKeys = json.encodeQueryValue(keys)
      if (encodedKeys.length() > MAX_QUERY_KEY_LENGTH) {
        doPost = true
        postBody['keys'] = keys
      } else {
        query.add("keys=${encodedKeys}")
      }
    }
    String queryAsString = query.join("&")

    Request.Builder builder = new Request.Builder()
        .url(getBaseUrl() +
            "/${db.toLowerCase()}" +
            "/${designDocId}" +
            "/_view/${viewName}" +
            "?${queryAsString}")
    if (doPost) {
      String documentAsJson = json.encodeDocument(postBody)
      builder = builder.post(RequestBody.create(documentAsJson, parse("application/json")))
    } else {
      builder = builder.get()
    }
    if (couchdbUsername && couchdbPassword) {
      builder = builder.header("Authorization", Credentials.basic(couchdbUsername, couchdbPassword))
    }
    Request request = builder.build()

    Response response = client.newCall(request).execute()
    if (!response.successful) {
      if (response.body().contentLength() > 0) {
        log.error("error querying view: {}/{}: {}", response.code(), response.message(), response.body().string())
      } else {
        log.error("error querying view: {}/{}", response.code(), response.message())
      }
      throw new IllegalStateException("could not query view")
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
      if (response.body().contentLength() > 0) {
        log.error("error querying all_docs: {}/{}: {}", response.code(), response.message(), response.body().string())
      } else {
        log.error("error querying all_docs: {}/{}", response.code(), response.message())
      }
      throw new IllegalStateException("could not query all_docs")
    } else {
      def allDocs = json.consume(response.body().byteStream(), Map)
      allDocs = allDocs.rows.collect { row -> includeDocs ? row.doc : row }
      if (!includeDesignDoc) {
        allDocs = allDocs.grep {
//          if (it._id?.startsWith("_design/") || it.id?.startsWith("_design/")) {
//            log.info("removing $it")
//          }
          !it._id?.startsWith("_design/") && !it.id?.startsWith("_design/")
        }
      }
      return allDocs
    }
  }

  <T extends RowReference<String>, R extends NonReducedViewQueryResponse<String, T>> R getAllDocs(
      Type R, String db, String startkey, String startkeyDocId,
      Integer limit = null, boolean includeDocs = true) {
    List<String> query = []
    if (includeDocs) {
      query.add("include_docs=${includeDocs}")
    }
    if (startkey) {
      query.add("startkey=${urlEncode(json.encodeQueryValue(startkey))}")
    }
    if (startkeyDocId) {
      String docId = sanitizeDocId(startkeyDocId)
      query.add("startkey_docid=${docId}")
    }
    if (limit != null) {
      query.add("limit=${limit}")
    }
    String queryAsString = query.join("&")

    Request.Builder builder = new Request.Builder()
        .url("${getBaseUrl()}/${db.toLowerCase()}" +
            "/_all_docs" +
            "?${queryAsString}")
        .get()
    if (couchdbUsername && couchdbPassword) {
      builder = builder.header("Authorization", Credentials.basic(couchdbUsername, couchdbPassword))
    }
    Request request = builder.build()

    Response response = client.newCall(request).execute()

    if (!response.successful) {
      if (response.body().contentLength() > 0) {
        log.error("error querying all_docs: {}/{}: {}", response.code(), response.message(), response.body().string())
      } else {
        log.error("error querying all_docs: {}/{}", response.code(), response.message())
      }
      throw new IllegalStateException("could not query all_docs")
    } else {
      R allDocs = json.consume(response.body().byteStream(), R)
      return allDocs
    }
  }

  <R> R queryPage(
      Type R, String db, String designDocId, String viewName, boolean reduce,
      Object startkey, String startkeyDocId,
      Integer skip = null, Integer limit = null,
      boolean includeDocs = false, boolean includeDesignDoc = false,
      Object endkey = null, String endkeyDocId = null,
      boolean doPost = false) {

    List<String> query = []
    query.add("reduce=${reduce}")
    if (reduce) {
      query.add("group=true")
    }
    if (!reduce && includeDocs) {
      query.add("include_docs=${includeDocs}")
    }
    Map postBody = [:]
    if (startkey) {
      // TODO allow non-String and non-Collection<String> instances for startkey
      String encodedKey = urlEncode(json.encodeQueryValue(startkey))
      if (encodedKey.length() > MAX_QUERY_KEY_LENGTH) {
        doPost = true
        postBody['startkey'] = startkey
      } else {
        query.add("startkey=${encodedKey}")
      }
    }
    if (startkeyDocId) {
      String docId = sanitizeDocId(startkeyDocId)
      query.add("startkey_docid=${docId}")
    }
    if (endkey) {
      // TODO allow non-String and non-Collection<String> instances for endkey
      String encodedKey = urlEncode(json.encodeQueryValue(endkey))
      if (encodedKey.length() > MAX_QUERY_KEY_LENGTH) {
        doPost = true
        postBody['endkey'] = endkey
      } else {
        query.add("endkey=${encodedKey}")
      }
    }
    if (endkeyDocId) {
      String docId = sanitizeDocId(endkeyDocId)
      query.add("endkey_docid=${docId}")
    }
    if (skip != null) {
      query.add("skip=${skip}")
    }
    if (limit != null) {
      query.add("limit=${limit}")
    }
    String queryAsString = query.join("&")

    Request.Builder builder = new Request.Builder()
        .url("${getBaseUrl()}/${db.toLowerCase()}" +
            "/${designDocId}" +
            "/_view/${viewName}" +
            "?${queryAsString}")
    if (doPost) {
      String documentAsJson = json.encodeDocument(postBody)
      builder = builder.post(RequestBody.create(documentAsJson, parse("application/json")))
    } else {
      builder = builder.get()
    }
    if (couchdbUsername && couchdbPassword) {
      builder = builder.header("Authorization", Credentials.basic(couchdbUsername, couchdbPassword))
    }
    Request request = builder.build()

    Response response = client.newCall(request).execute()

    if (!response.successful) {
      if (response.body().contentLength() > 0) {
        log.error("error querying view: {}/{}: {}", response.code(), response.message(), response.body().string())
      } else {
        log.error("error querying view: {}/{}", response.code(), response.message())
      }
      throw new IllegalStateException("could not query view")
    } else {
      R allDocs = json.consume(response.body().byteStream(), R)
      if (!reduce && !includeDesignDoc) {
        allDocs.rows.removeIf { it.id?.startsWith("_design/") }
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
    } else {
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
      } else if (!document.dateCreated) {
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
      try {
        log.error("error {}/{}, details: {}", response.code(), response.message(), response.body().string())
      } catch (Exception ignored) {
        log.error("error {}/{}", response.code(), response.message())
      }
      throw new IllegalStateException("bulk update failed")
    }

    List result = json.consume(response.body().byteStream(), List)
    [result, documents].transpose().each { updated, original ->
      if (updated.ok) {
        original._id = updated.id
        original._rev = updated.rev
        return original
      } else {
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
      } else {
        log.error("error {}", response.message())
        throw new IllegalStateException("could not check for database existence")
      }
    } else {
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
    String designDocId = "_design/${db.capitalize()}"
    return createView(db, designDocId, viewName, mapFunction, reduceFunction)
  }

  Map createView(String db, String designDocId, String viewName, String mapFunction, String reduceFunction) {
    Map view = [:]
    if (mapFunction) {
      view['map'] = mapFunction
    }
    if (reduceFunction) {
      view['reduce'] = reduceFunction
    }

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
    } else {
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
      } else {
        log.error("error {}", response.message())
        throw new IllegalStateException("could not check for doc existence")
      }
    } else {
      return true
    }
  }

  Map getDbInfo(String db) {
    Request.Builder builder = new Request.Builder()
        .url("${getBaseUrl()}/${db.toLowerCase()}")
        .get()
    if (couchdbUsername && couchdbPassword) {
      builder = builder.header("Authorization", Credentials.basic(couchdbUsername, couchdbPassword))
    }
    Request request = builder.build()

    Response response = client.newCall(request).execute()

    if (!response.successful) {
      log.error("error getting db info({}): {}/{}", db, response.code(), response.message())
      throw new IllegalStateException("could not get db info for '${db}'")
    } else {
      Map doc = json.consume(response.body().byteStream(), Map)
      return doc
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
    } else {
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
    } else {
      Map result = json.consume(response.body().byteStream(), Map)
      return result
    }
  }

  static Map merge(Map currentDoc, Map changedDoc) {
    def mergedDoc = currentDoc + [:]
    mergedDoc.views = (currentDoc.views ?: [:]) + (changedDoc.views ?: [:])
    return mergedDoc
  }

  static String sanitizeDocId(String docId) {
    if (!docId.startsWith('_')) {
      docId = urlEncode(docId)
    }
    docId
  }

  static String urlEncode(String value) {
    return URLEncoder.encode(value, UTF_8.toString())
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
