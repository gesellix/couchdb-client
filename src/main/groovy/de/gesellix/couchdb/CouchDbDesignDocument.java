package de.gesellix.couchdb;

import java.util.Map;

public interface CouchDbDesignDocument {

  String getId();

  String getRevision();

  Map<String, Object> getViews();

  void setViews(Map<String, Object> views);
}
