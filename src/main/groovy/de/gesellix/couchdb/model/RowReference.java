package de.gesellix.couchdb.model;

public interface RowReference<KeyType> {

  KeyType getKey();

  String getDocId();
}
