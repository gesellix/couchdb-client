package de.gesellix.couchdb

import de.gesellix.couchdb.model.MapWithDocumentId
import de.gesellix.couchdb.model.RowReference
import de.gesellix.couchdb.model.ViewQueryResponse
import de.gesellix.couchdb.moshi.MoshiViewQueryResponse
import de.gesellix.couchdb.moshi.MoshiViewQueryResponseRow
import spock.lang.Specification

class PagedViewIteratorSpec extends Specification {

  def "hasNext defaults to 'true'"() {
    given:
    PagedViewIterator<String, MoshiViewQueryResponseRow<String, String, MapWithDocumentId<Object>>> iterator
        = new PagedViewIterator<String, MoshiViewQueryResponseRow<String, String, MapWithDocumentId<Object>>>(2, { nextPage ->
      return new MoshiViewQueryResponse<String, String, MapWithDocumentId<Object>>()
    })

    expect:
    iterator.hasNext()
  }

  def "should start with a null startkey"() {
    given:
    List requests = []
    PagedViewIterator<String, MoshiViewQueryResponseRow<String, String, MapWithDocumentId<Object>>> iterator
        = new PagedViewIterator<String, MoshiViewQueryResponseRow<String, String, MapWithDocumentId<Object>>>(2, { nextPage, limit ->
      requests << nextPage
      return new MoshiViewQueryResponse<String, String, MapWithDocumentId<Object>>()
    })

    when:
    iterator.next()

    then:
    !iterator.hasNext()
    requests == [null]
  }

  def "should continue with the last docid as next startkey"() {
    given:
    List requests = []
    PagedViewIterator<String, MoshiViewQueryResponseRow<String, String, MapWithDocumentId<Object>>> iterator
        = new PagedViewIterator<String, MoshiViewQueryResponseRow<String, String, MapWithDocumentId<Object>>>(2, { nextPage, limit ->
      requests << nextPage

      def response = new MoshiViewQueryResponse<String, String, MapWithDocumentId<Object>>(
          totalRows: 1,
          offset: 0,
          rows: [new MoshiViewQueryResponseRow<String, String, MapWithDocumentId<Object>>(
              id: "docid-${requests.size()}"
          )]
      )
      return response
    })

    when:
    def page1 = iterator.next()

    then:
    !iterator.hasNext()
    requests == [null]
    page1.rows.collect { it.id } == ["docid-1"]
  }

  def "should iterate over multiple pages"() {
    given:
    List database = [
        [_id: "docid-1", key: "docid-1"],
        [_id: "docid-2", key: "docid-2"],
        [_id: "docid-3", key: "docid-3"],
        [_id: "docid-4", key: "docid-4"],
        [_id: "docid-5", key: "docid-5"],
        [_id: "docid-6", key: "docid-6"],
        [_id: "docid-7", key: "docid-7"]
    ]

    List<RowReference> requests = []
    def pageProvider = { RowReference nextPage, Integer limit ->
      requests << nextPage

      int startindex = 0
      if (nextPage != null) {
        startindex = database.findIndexOf { it.key == nextPage.key }
      }
      int endindex = Math.min(database.size(), startindex + limit ?: 0)

      println("sublist(${startindex}, ${endindex})")
      def rows = database.subList(startindex, endindex).collect {
        def wrapped = new MapWithDocumentId(it)
        new MoshiViewQueryResponseRow<String, String, MapWithDocumentId<Object>>(
            id: wrapped.get("_id"),
            key: wrapped.get("key"),
            doc: wrapped
        )
      }
      def response = new MoshiViewQueryResponse<String, String, MapWithDocumentId<Object>>(
          totalRows: database.size(),
          offset: startindex,
          rows: rows
      )
      return response
    }
    PagedViewIterator<String, MoshiViewQueryResponseRow<String, String, MapWithDocumentId<Object>>> iterator
        = new PagedViewIterator<String, MoshiViewQueryResponseRow<String, String, MapWithDocumentId<Object>>>(2, pageProvider)

    when:
    List<ViewQueryResponse<String, MoshiViewQueryResponseRow<String, String, MapWithDocumentId<Object>>>> pages = []
    while (iterator.hasNext()) {
      def page = iterator.next()
      pages << page
      println("page: ${page.rows.collect { it.id }}")
    }

    then:
    !iterator.hasNext()
    pages.collect { it.rows }.flatten().size() == database.size()
    requests == [
        null,
        [id: "docid-3", key: "docid-3"] as MoshiViewQueryResponseRow,
        [id: "docid-5", key: "docid-5"] as MoshiViewQueryResponseRow,
        [id: "docid-7", key: "docid-7"] as MoshiViewQueryResponseRow,
    ]
  }
}
