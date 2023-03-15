package de.gesellix.couchdb.model;

public interface RowReference<KeyType> {

  KeyType getKey();

  String getDocId();

  static String toString(RowReference<?> row) {
    if (row == null) {
      return "null";
    }
    return String.format("key=%s, docId=%s", row.getKey(), row.getDocId());
  }
}
