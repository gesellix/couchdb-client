package de.gesellix.couchdb;

public interface ViewQueryResponseRow<DocType> {

  String getId();

  String getRev();

  /**
   * available when include_docs == true
   *
   * @return the document
   */
  DocType getDoc();
}
