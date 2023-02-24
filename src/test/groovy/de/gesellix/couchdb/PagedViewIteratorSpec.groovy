package de.gesellix.couchdb

import de.gesellix.couchdb.moshi.MoshiViewQueryResponse
import de.gesellix.couchdb.moshi.MoshiViewQueryResponseRow
import spock.lang.Specification

class PagedViewIteratorSpec extends Specification {

  def "hasNext defaults to 'true'"() {
    given:
    PagedViewIterator<Map> iterator = new PagedViewIterator<Map>(2, { startkey ->
      return new MoshiViewQueryResponse<Map>()
    })

    expect:
    iterator.hasNext()
  }

  def "should start with a null startkey"() {
    given:
    List requests = []
    PagedViewIterator<Map> iterator = new PagedViewIterator<Map>(2, { startkey, limit ->
      requests << startkey
      return new MoshiViewQueryResponse<Map>()
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
    PagedViewIterator<Map> iterator = new PagedViewIterator<Map>(2, { startkey, limit ->
      requests << startkey

      def response = new MoshiViewQueryResponse<Map>(
          totalRows: 1,
          offset: 0,
          rows: [new MoshiViewQueryResponseRow<Map>(
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
        [id: "docid-1"],
        [id: "docid-2"],
        [id: "docid-3"],
        [id: "docid-4"],
        [id: "docid-5"],
        [id: "docid-6"],
        [id: "docid-7"]
    ]

    List requests = []
    PagedViewIterator<Map> iterator = new PagedViewIterator<Map>(2, { startkey, limit ->
      requests << startkey

      int startindex = 0
      if (startkey != null) {
        startindex = database.findIndexOf { it.id == startkey }
      }
      int endindex = Math.min(database.size(), startindex + limit)

      println("sublist(${startindex}, ${endindex})")
      def rows = database.subList(startindex, endindex).collect {
        new MoshiViewQueryResponseRow<Map>(
            id: it.get('id'),
            doc: it,
        )
      }
      def response = new MoshiViewQueryResponse<Map>(
          totalRows: database.size(),
          offset: startindex,
          rows: rows
      )
      return response
    })

    when:
    List<MoshiViewQueryResponse<Map>> pages = []
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
        "docid-3",
        "docid-5",
//        "docid-7",
    ]
  }
}
