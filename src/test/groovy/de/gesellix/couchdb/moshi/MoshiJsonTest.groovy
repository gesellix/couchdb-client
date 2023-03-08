package de.gesellix.couchdb.moshi

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import de.gesellix.couchdb.RowWithAuthor
import de.gesellix.couchdb.RowWithAuthorAdapter
import de.gesellix.couchdb.model.MapWithDocumentId
import de.gesellix.couchdb.model.ViewQueryResponse
import spock.lang.Specification

import java.lang.reflect.Type

class MoshiJsonTest extends Specification {

  def "should parse a Map"() {
    given:
    String body = """
    {
      "rows" : [
        {"key": null,"value": true}
      ]
    }
    """

    when:
    def parsed = new MoshiJson().consume(new ByteArrayInputStream(body.bytes), Map)

    then:
    parsed.rows[0] == [key: null, value: true]
  }

  def "should parse a MoshiViewQueryResponse"() {
    given:
    String body = """
    {
      "total_rows": 9,
      "offset": 0,
      "rows": [
        {"id":"test-id/03467a76-01aa-431c-b3c4-17977728a1a7","key":"A not so unique title","value":"test-id/03467a76-01aa-431c-b3c4-17977728a1a7","doc":{"_id":"test-id/03467a76-01aa-431c-b3c4-17977728a1a7","_rev":"1-b0c26e203eba95124a78257df038458f","a-property":"a-value-1","title":"A not so unique title","dateCreated":"2023-03-07"}},
        {"id":"test-id/08864933-04df-49cc-9264-257e14c8c894","key":"A not so unique title","value":"test-id/08864933-04df-49cc-9264-257e14c8c894","doc":{"_id":"test-id/08864933-04df-49cc-9264-257e14c8c894","_rev":"1-e98aae66d0b1c1fdf3b9289633290ef9","a-property":"a-value-3","title":"A not so unique title","dateCreated":"2023-03-07"}},
        {"id":"test-id/983991e6-7be7-4ebc-bb04-74a42c41dcb9","key":"A not so unique title","value":"test-id/983991e6-7be7-4ebc-bb04-74a42c41dcb9","doc":{"_id":"test-id/983991e6-7be7-4ebc-bb04-74a42c41dcb9","_rev":"1-e98aae66d0b1c1fdf3b9289633290ef9","a-property":"a-value-3","title":"A not so unique title","dateCreated":"2023-03-07"}},
        {"id":"test-id/aa6722dc-f811-4711-8582-360a58a64c57","key":"A not so unique title","value":"test-id/aa6722dc-f811-4711-8582-360a58a64c57","doc":{"_id":"test-id/aa6722dc-f811-4711-8582-360a58a64c57","_rev":"1-b0c26e203eba95124a78257df038458f","a-property":"a-value-1","title":"A not so unique title","dateCreated":"2023-03-07"}},
        {"id":"test-id/e7ba7fdc-63b7-4cb5-ba41-56a04b6dcf1a","key":"A not so unique title","value":"test-id/e7ba7fdc-63b7-4cb5-ba41-56a04b6dcf1a","doc":{"_id":"test-id/e7ba7fdc-63b7-4cb5-ba41-56a04b6dcf1a","_rev":"1-b0c26e203eba95124a78257df038458f","a-property":"a-value-1","title":"A not so unique title","dateCreated":"2023-03-07"}},
        {"id":"test-id/f2f372ef-7e79-469c-a308-5bb266440f7e","key":"A not so unique title","value":"test-id/f2f372ef-7e79-469c-a308-5bb266440f7e","doc":{"_id":"test-id/f2f372ef-7e79-469c-a308-5bb266440f7e","_rev":"1-e98aae66d0b1c1fdf3b9289633290ef9","a-property":"a-value-3","title":"A not so unique title","dateCreated":"2023-03-07"}},
        {"id":"test-id/45a41d27-43d2-4b49-a9fb-7d18f047b1ff","key":"A quite unique title","value":"test-id/45a41d27-43d2-4b49-a9fb-7d18f047b1ff","doc":{"_id":"test-id/45a41d27-43d2-4b49-a9fb-7d18f047b1ff","_rev":"1-fe8b97ce9c0eaefb066ed2253e6ef44a","a-property":"a-value-2","title":"A quite unique title","dateCreated":"2023-03-07"}},
        {"id":"test-id/55baff8e-2213-409f-8340-124798c469fe","key":"A quite unique title","value":"test-id/55baff8e-2213-409f-8340-124798c469fe","doc":{"_id":"test-id/55baff8e-2213-409f-8340-124798c469fe","_rev":"1-fe8b97ce9c0eaefb066ed2253e6ef44a","a-property":"a-value-2","title":"A quite unique title","dateCreated":"2023-03-07"}},
        {"id":"test-id/70a0f80c-4462-47b0-97e3-2b50b62df530","key":"A quite unique title","value":"test-id/70a0f80c-4462-47b0-97e3-2b50b62df530","doc":{"_id":"test-id/70a0f80c-4462-47b0-97e3-2b50b62df530","_rev":"1-fe8b97ce9c0eaefb066ed2253e6ef44a","a-property":"a-value-2","title":"A quite unique title","dateCreated":"2023-03-07"}}
      ]
    }
    """

    Type docType = Types.newParameterizedType(MapWithDocumentId, Object)
    Type resultType = Types.newParameterizedType(MoshiViewQueryResponse, String, String, docType)

    when:
    ViewQueryResponse<String, MoshiViewQueryResponseRow<String, String, MapWithDocumentId<Object>>> parsed = new MoshiJson(
        new Moshi.Builder().add(new MapWithDocumentIdAdapter())
    ).consume(new ByteArrayInputStream(body.bytes), resultType)

    then:
    parsed.rows.collect { it.key }.contains("A not so unique title")
    parsed.rows.collect { it.value }.contains("test-id/70a0f80c-4462-47b0-97e3-2b50b62df530")
    parsed.rows.collect { it.doc.getId() }.contains("test-id/08864933-04df-49cc-9264-257e14c8c894")
  }

  def "should parse a boring MoshiReducedViewQueryResponse"() {
    given:
    String body = """
    {
      "rows": [
        {"key":null, "value":true}
      ]
    }
    """

    Type rowType = Types.newParameterizedType(Map, String, Boolean)
    Type resultType = Types.newParameterizedType(MoshiReducedViewQueryResponse, String, rowType)

    when:
    MoshiReducedViewQueryResponse parsed = new MoshiJson().consume(new ByteArrayInputStream(body.bytes), resultType)

    then:
    parsed.rows.size() == 1
    parsed.rows[0] == [key: null, value: true]
  }

  def "should parse an exciting MoshiReducedViewQueryResponse"() {
    given:
    String body = """
    {
      "rows": [
        {"key":"A. A. Milne", "value":true},
        {"key":"A. Powell Davies", "value":true},
        {"key":"Abernathy", "value":true},
        {"key":"Abraham Lincoln", "value":true},
        {"key":"Abraham Maslow", "value":true},
        {"key":"Aesop", "value":true},
        {"key":"African proverb", "value":true},
        {"key":"Agatha Christie", "value":true},
        {"key":"Ajahn Chah", "value":true},
        {"key":"Alan Cohen", "value":true},
        {"key":"Alan Watts", "value":true}
      ]
    }
    """

    Type resultType = Types.newParameterizedType(MoshiReducedViewQueryResponse, String, RowWithAuthor)

    when:
    MoshiReducedViewQueryResponse<String, RowWithAuthor> parsed = new MoshiJson(new Moshi.Builder()
        .add(new RowWithAuthorAdapter()))
        .consume(new ByteArrayInputStream(body.bytes), resultType)

    then:
    parsed.rows.size() == 11
    parsed.rows[0] == new RowWithAuthor([key: "A. A. Milne", value: true])
  }
}
