package de.gesellix.couchdb.model;

public interface NonReducedViewQueryResponse<KeyType, Row extends RowReference<KeyType>> extends ViewQueryResponse<KeyType, Row> {

  /**
   * Available when not reduced
   */
  Integer getOffset();

  /**
   * Available when not reduced
   */
  Integer getTotalRows();
}
