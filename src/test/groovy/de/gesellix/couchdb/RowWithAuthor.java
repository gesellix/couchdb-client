package de.gesellix.couchdb;

import de.gesellix.couchdb.model.MapWithRowReference;

import java.util.Map;

public class RowWithAuthor extends MapWithRowReference<String, Object> {

  public RowWithAuthor(Map<String, Object> delegate) {
    super(delegate);
  }

  @Override
  public String getKey() {
    return (String) get("key");
  }
}
