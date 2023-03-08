package de.gesellix.couchdb.model;

import java.util.List;

public interface ViewQueryResponse<KeyType, Row extends RowReference<KeyType>> {

  List<? extends Row> getRows();
}
