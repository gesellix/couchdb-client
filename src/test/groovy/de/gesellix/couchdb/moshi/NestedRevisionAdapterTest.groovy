package de.gesellix.couchdb.moshi

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import de.gesellix.couchdb.model.MapWithDocumentId
import spock.lang.Specification

import java.lang.reflect.Type

class NestedRevisionAdapterTest extends Specification {

  def "decode nested rev property"() {
    given:
    String body = '{"total_rows":26, "rows": [{"id":"42", "value":{"rev":"5555"}, "doc":{"_id":"42"}}]}'
    Type resultType = Types.newParameterizedType(
        MoshiAllDocsViewQueryResponse, Types.newParameterizedType(
        MapWithDocumentId, Object))

    when:
    MoshiAllDocsViewQueryResponse<MapWithDocumentId> result = new Moshi.Builder()
        .add(new NestedRevisionAdapter())
        .add(new MapWithDocumentIdAdapter())
        .build()
        .adapter(resultType)
        .fromJson(body)

    then:
    result.totalRows == 26
    result.rows.first().id == "42"
    result.rows.first().rev == "5555"
    result.rows.first().doc == new MapWithDocumentId([_id: "42"])
  }

  def "decode nested rev and doc properties"() {
    given:
    String body = '{"total_rows":26, "rows": [{"id":"42", "value":{"rev":"5555"}, "doc":{"_id":"42", "_rev":"5555", "propertyName":"propertyValue"}}]}'
    Type docType = Types.newParameterizedType(MapWithDocumentId, Object)
    Type resultType = Types.newParameterizedType(MoshiAllDocsViewQueryResponse, docType)

    when:
    MoshiAllDocsViewQueryResponse<MapWithDocumentId<Object>> result = new Moshi.Builder()
        .add(new NestedRevisionAdapter())
        .add(new MapWithDocumentIdAdapter())
        .build()
        .adapter(resultType)
        .fromJson(body)

    then:
    result.totalRows == 26
    result.rows.first().id == "42"
    result.rows.first().rev == "5555"
    result.rows.first().doc == new MapWithDocumentId([_id: "42", _rev: "5555", propertyName: "propertyValue"])
  }
}
