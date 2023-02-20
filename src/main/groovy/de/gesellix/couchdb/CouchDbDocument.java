package de.gesellix.couchdb;

public interface CouchDbDocument {

  String getId();

  String getRevision();

  String getDateCreated();

  void setDateCreated(Object String);

  String getDateUpdated();

  void setDateUpdated(Object String);
}
