package de.gesellix.couchdb.moshi

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import spock.lang.Specification

import java.lang.reflect.Type

class NestedRevisionAdapterTest extends Specification {

  def "decode nested rev property"() {
    given:
    String body = '{"total_rows":26, "rows": [{"id":"42", "value":{"rev":"5555"}}]}'
    Type resultType = Types.newParameterizedType(
        MoshiViewQueryResponse, Types.newParameterizedType(
        Map, String, Object))

    when:
    MoshiViewQueryResponse<Map> result = new Moshi.Builder()
        .add(new NestedRevisionAdapter())
        .build()
        .adapter(resultType)
        .fromJson(body)

    then:
    result.totalRows == 26
    result.rows.first().id == "42"
    result.rows.first().rev == "5555"
  }

  def "decode nested rev and doc properties"() {
    given:
    String body = '{"total_rows":26, "rows": [{"id":"42", "value":{"rev":"5555"}, "doc":{"_id":"42", "_rev":"5555", "propertyName":"propertyValue"}}]}'
    Type resultType = Types.newParameterizedType(
        MoshiViewQueryResponse, Types.newParameterizedType(
        Map, String, Object))

    when:
    MoshiViewQueryResponse<Map<String, Object>> result = new Moshi.Builder()
        .add(new NestedRevisionAdapter())
        .build()
        .adapter(resultType)
        .fromJson(body)

    then:
    result.totalRows == 26
    result.rows.first().id == "42"
    result.rows.first().rev == "5555"
    result.rows.first().doc == [_id: "42", _rev: "5555", propertyName: "propertyValue"]
  }
}
