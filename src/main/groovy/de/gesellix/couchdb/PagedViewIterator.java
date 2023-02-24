package de.gesellix.couchdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;

public class PagedViewIterator<T> implements Iterator<ViewQueryResponse<T>> {

  private static final Logger log = LoggerFactory.getLogger(PagedViewIterator.class);

  private final int pageSize;
  private final BiFunction<String, Integer, ViewQueryResponse<T>> pageProvider;

  String nextPageStartkeyDocId;

  ViewQueryResponse<T> lastResult;

  public PagedViewIterator(int pageSize, BiFunction<String, Integer, ViewQueryResponse<T>> pageProvider) {
    this.pageSize = pageSize;
    this.pageProvider = pageProvider;
    this.nextPageStartkeyDocId = null;
    this.lastResult = null;
  }

  @Override
  public boolean hasNext() {
    if (lastResult == null) {
      // we didn't even try, yet
      return true;
    }
    return nextPageStartkeyDocId != null;
  }

  @Override
  public ViewQueryResponse<T> next() {
    if (!hasNext()) {
      throw new NoSuchElementException("no more pages available");
    }
    ViewQueryResponse<T> fetched = fetch();
    return fetched;
  }

  private ViewQueryResponse<T> fetch() {
    lastResult = pageProvider.apply(nextPageStartkeyDocId, pageSize + 1);
    if (lastResult == null || lastResult.getRows() == null) {
      throw new IllegalStateException("failed to fetch more rows. nextPageStartkeyDocId(" + nextPageStartkeyDocId + ")");
    }
    log.info("got result, totalRows({}), offset({}), rows({}), startkeyDocId({})",
        lastResult.getTotalRows(), lastResult.getOffset(), lastResult.getRows().size(), nextPageStartkeyDocId);

    if (lastResult.getRows().size() == 0
        || lastResult.getRows().size() >= (lastResult.getTotalRows() - lastResult.getOffset())) {
      // finished
      updateNextPageStartkeyDocId(null);
    } else {
      // safe for the next iteration
      ViewQueryResponseRow<T> trailingRow = lastResult.getRows().remove(lastResult.getRows().size() - 1);
      updateNextPageStartkeyDocId(trailingRow.getId());
    }
    return lastResult;
  }

  private void updateNextPageStartkeyDocId(String nextPageStartkeyDocId) {
    if (Objects.equals(this.nextPageStartkeyDocId, nextPageStartkeyDocId)) {
      int offset = lastResult == null || lastResult.getOffset() == null ? -1 : lastResult.getOffset();
      log.warn("startkeyDocId hasn't changed! previous({}) == next({}), lastResult.offset({})",
          this.nextPageStartkeyDocId,
          nextPageStartkeyDocId,
          offset);
      // throw exception?
      nextPageStartkeyDocId = null;
    }
    this.nextPageStartkeyDocId = nextPageStartkeyDocId;
  }
}
