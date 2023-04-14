package de.gesellix.couchdb;

import de.gesellix.couchdb.model.MapWithRowReference;

import java.util.Map;

public class RowWithStringKey extends MapWithRowReference<String, Object> {

  public RowWithStringKey(Map<String, Object> delegate) {
    super(delegate);
  }

  @Override
  public String getKey() {
    return (String) get("key");
  }
}
