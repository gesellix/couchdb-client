package de.gesellix.couchdb;

import de.gesellix.couchdb.model.MapWithRowReference;

import java.util.List;
import java.util.Map;

public class RowWithComplexKey extends MapWithRowReference<List<String>, Object> {

  public RowWithComplexKey(Map<String, Object> delegate) {
    super(delegate);
  }

  @Override
  public List<String> getKey() {
    return (List<String>) get("key");
  }
}
