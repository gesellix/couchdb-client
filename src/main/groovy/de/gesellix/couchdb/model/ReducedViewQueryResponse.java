package de.gesellix.couchdb.model;

public interface ReducedViewQueryResponse<KeyType, Row extends RowReference<KeyType>>
    extends ViewQueryResponse<KeyType, Row> {
}
