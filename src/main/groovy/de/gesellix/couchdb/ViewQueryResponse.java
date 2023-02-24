package de.gesellix.couchdb;

import java.util.List;

public interface ViewQueryResponse<DocType> {

  Integer getOffset();

  Integer getTotalRows();

  List<? extends ViewQueryResponseRow<DocType>> getRows();
}
