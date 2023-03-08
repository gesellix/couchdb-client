package de.gesellix.couchdb.model;

public interface NonReducedViewQueryResponseRow<KeyType, ValueType, DocType extends DocumentId>
    extends RowReference<KeyType> {

  String getId();

  KeyType getKey();

  ValueType getValue();

  /**
   * Available when include_docs == true
   */
  DocType getDoc();
}
