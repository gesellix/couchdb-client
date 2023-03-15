package de.gesellix.couchdb;

import de.gesellix.couchdb.model.NonReducedViewQueryResponse;
import de.gesellix.couchdb.model.RowReference;
import de.gesellix.couchdb.model.ViewQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;

public class PagedViewIterator<KeyType, Row extends RowReference<KeyType>>
    implements Iterator<ViewQueryResponse<KeyType, Row>> {

  private static final Logger log = LoggerFactory.getLogger(PagedViewIterator.class);

  private final int pageSize;
  private final BiFunction<RowReference<KeyType>, Integer, ViewQueryResponse<KeyType, Row>> pageProvider;

  RowReference<KeyType> nextPage;

  ViewQueryResponse<KeyType, Row> lastResult;

  public PagedViewIterator(int pageSize, BiFunction<RowReference<KeyType>, Integer, ViewQueryResponse<KeyType, Row>> pageProvider) {
    this.pageSize = pageSize;
    this.pageProvider = pageProvider;
    this.nextPage = null;
    this.lastResult = null;
  }

  @Override
  public boolean hasNext() {
    if (lastResult == null) {
      // we didn't even try, yet
      return true;
    }
    return nextPage != null;
  }

  @Override
  public ViewQueryResponse<KeyType, Row> next() {
    if (!hasNext()) {
      throw new NoSuchElementException("no more pages available");
    }
    ViewQueryResponse<KeyType, Row> fetched = fetch();
    return fetched;
  }

  private ViewQueryResponse<KeyType, Row> fetch() {
    lastResult = pageProvider.apply(nextPage, pageSize + 1);
    if (lastResult == null || lastResult.getRows() == null) {
      throw new IllegalStateException("failed to fetch more rows. nextPage(" + RowReference.toString(nextPage) + ")");
    }
    if (lastResult instanceof NonReducedViewQueryResponse) {
      NonReducedViewQueryResponse<?, ?> nonReducedLastResult = (NonReducedViewQueryResponse<?, ?>) lastResult;
      log.info("got result, totalRows({}), offset({}), rows({}), nextPage({})",
          nonReducedLastResult.getTotalRows(), nonReducedLastResult.getOffset(), lastResult.getRows().size(), RowReference.toString(nextPage));
    } else {
      log.info("got result, rows({}), nextPage({})",
          lastResult.getRows().size(), RowReference.toString(nextPage));
    }

    if (lastResult.getRows().size() <= pageSize) {
      // finished
      updateNextPage(null);
    } else {
      // safe for the next iteration
      Row trailingRow = lastResult.getRows().remove(lastResult.getRows().size() - 1);
      updateNextPage(trailingRow);
    }
    return lastResult;
  }

  private void updateNextPage(RowReference<KeyType> nextPage) {
    if (Objects.equals(this.nextPage, nextPage)) {
      if (lastResult instanceof NonReducedViewQueryResponse) {
        NonReducedViewQueryResponse<?, ?> nonReducedLastResult = (NonReducedViewQueryResponse<?, ?>) lastResult;
        int offset = nonReducedLastResult.getOffset() == null ? -1 : nonReducedLastResult.getOffset();
        log.info("nextPage hasn't changed: previous({}) == next({}), lastResult.offset({})",
            RowReference.toString(this.nextPage),
            RowReference.toString(nextPage),
            offset);
      } else {
        log.info("nextPage hasn't changed: previous({}) == next({})",
            RowReference.toString(this.nextPage),
            RowReference.toString(nextPage));
      }
      // throw exception?
      nextPage = null;
    }
    this.nextPage = nextPage;
  }
}
