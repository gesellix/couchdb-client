package de.gesellix.couchdb.model;

import java.util.HashMap;
import java.util.Map;

public abstract class MapWithRowReference<T, V> extends HashMap<String, V> implements RowReference<T> {

  public MapWithRowReference(Map<String, V> delegate) {
    super(delegate);
  }

  @Override
  public String getDocId() {
    return null;
  }
}
