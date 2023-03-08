package de.gesellix.couchdb.model;

import java.util.HashMap;
import java.util.Map;

public class MapWithDocumentId<V> extends HashMap<String, V> implements DocumentId {

  public MapWithDocumentId(Map<String, V> delegate) {
    super(delegate);
  }

  @Override
  public String getId() {
    return (String) get("_id");
  }
}
